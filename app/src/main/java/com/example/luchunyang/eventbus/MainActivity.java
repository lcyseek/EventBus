package com.example.luchunyang.eventbus;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import java.util.Timer;
import java.util.TimerTask;

/**
 *  EventBus优缺点：
 优点：简化组件之间的通信方式，实现解耦让业务代码更加简洁，可以动态设置事件处理线程以及优先级

 缺点：目前发现唯一的缺点就是类似之前策略模式一样的诟病，每个事件都必须自定义一个事件类，造成事件类太多，无形中加大了维护成本
 */
public class MainActivity extends AppCompatActivity {

    public static final String TAG = MainActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        EventBus.getDefault().register(this);
    }

    public void post(View view) {
//        EventBus.getDefault().post(new AsyncEvent("this is MainActivity event."));
        EventBus.getDefault().post("这是来自主线程的message");
    }

    public void postInThread(View view) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                //EventBus.getDefault().post(new AsyncEvent("this is MainActivity thread event."));
                EventBus.getDefault().post("这是来自子线程的message");
            }
        }).start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
    }

    //EventBus默认的模式，也就是说：post event和handle event在同一个线程
    @Subscribe(threadMode = ThreadMode.POSTING)
    public void onMessage(String message){
        int a = 10/0;
        //EventBus.getDefault().cancelEventDelivery(message) ;//优先级高的订阅者可以终止事件往下传递
        Log.i(TAG, "onMessage: "+Thread.currentThread().getName()+"--->"+message);
    }

    //运行在UI线程的 使用此模式，不能堵塞UI线程
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessage1(String message){
        Log.i(TAG, "onMessage1: "+Thread.currentThread().getName()+"--->"+message);
    }

    /**
     * 若当前线程非UI线程则在当前线程中执行，否则加入后台任务队列，使用线程池调用
     * Warning:使用此模式，不能堵塞后台线程
     */
    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    public void onMessage2(String message){
        Log.i(TAG, "onMessage2: "+Thread.currentThread().getName()+"--->"+message);
    }

    /**
     * handle event始终独立于UI线程和post event所在的线程，即：和post event 不在同一线程，
     * 也不在UI线程。此模式适合耗时任务：e.g.:网络链接。
     * Warning:避免触发大量的长时间运行的异步线程，来限制并发线程的数量。
     */
    @Subscribe(threadMode = ThreadMode.ASYNC)
    public void onMessage3(String message){
        Log.i(TAG, "onMessage3: "+Thread.currentThread().getName()+"--->"+message);
    }


    /*********************************************************************************/


    private Timer timer ;
    private TimerTask task;
    public void postTime(View view) {
        timer = new Timer();
        task = new TimerTask() {
            int flag = 0;
            @Override
            public void run() {
                Log.i(TAG, "run: 开始发送消息");
                EventBus.getDefault().post(new MyEvent("hello android--->"+(++flag)));
                Log.i(TAG, "run: 发送消息完毕");

            }
        };

        timer.schedule(task,2000,1000);
    }

    public void cancelPostTime(View view) {
        if(timer != null && task != null)
            timer.cancel();
    }

    public void postSticky(View view) {
        EventBus.getDefault().postSticky("this is sticky");
    }

    public void registForSticky(View view) {
        class Student{
            public Student() {
                EventBus.getDefault().register(this);
            }

            //前面说的一般的post event，正常流程，必须是先register,然后post event,handle event才能起作用。
            //但是当我们post event时，还没有subcriber怎么办？但是又想后注册的subscriber,能收到消息，这时sticky event就开始大显身手了。
            //EventBus会把最后一个sticky event记录在内存中，然后分发给subscriber
            @Subscribe(sticky = true,threadMode = ThreadMode.ASYNC)
            public void onMessage(String message){
                Log.i(TAG, "onMessage: "+message);
            }
        }

        Student s = new Student();
    }

    public void removeSticky(View view) {
        //返回的是之前的sticky event
        EventBus.getDefault().removeStickyEvent(String.class);
    }



    class MyEvent{
        private String message;

        public MyEvent(String message) {
            this.message = message;
        }

        @Override
        public String toString() {
            return "MyEvent{" +
                    "message='" + message + '\'' +
                    '}';
        }
    }

//    @Subscribe(threadMode = ThreadMode.BACKGROUND)
//    public void getEvent(MyEvent event){
//        //虽然是子线程,但是仍然会阻塞发送线程
//        Log.i(TAG, "getEvent: "+event);
//        try {
//            Thread.sleep(10000);
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
//
//        Log.i(TAG, "getEvent: end");
//    }

    @Subscribe(threadMode = ThreadMode.ASYNC)
    public void getEvent(MyEvent event){
        //这个不会阻塞发送线程
        Log.i(TAG, "getEvent: "+event);
        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        Log.i(TAG, "getEvent: end");
    }

    EventBus bus = null;
    public void customEventBus(View view) {
        //EventBus2.3新增使用EventBusBuilder 去配置各种不同的EventBus，例如去创建一个没有订阅者的EventBus
        //当订阅者执行失败时抛出异常的例子。注意：  EventBus捕获到从onEvent方法中抛出的异常，并发送一个SubscirberExceptionEvent，不过不一定要处理。

        //默认的EventBus 是会处理订阅者的异常的,可以自己配置不处理订阅者的异常,如下
        bus = EventBus.builder().throwSubscriberException(true).build();
    }

    public void send(View view) {
        bus.post("hello");
    }

    public void registCustomEventBus(View view) {
        class A{
            public A() {
            }

            @Subscribe
            public void custom(String message){
                System.out.println("收到自定义Bus : "+message);
                int a = 10/0;
            }
        }

        A a = new A();
        bus.register(a);
    }

}
