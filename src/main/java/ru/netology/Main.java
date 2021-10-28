package ru.netology;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

public class Main {
    private static final int TIMES_TO_CALL = 3;
    private static final int CALLS_COUNT = 60;
    private static final int TIME_BETWEEN_CALLS = 1_000;

    private static final int TIME_BETWEEN_CHECK_CALL = 100;
    private static final int TIME_TO_HANDLE_CALL = 3_000;
    private static final int OPERATORS_COUNT = 5;

    //Рассчитываем, что после этого количества уже нет смысла ставить звонки в очередь (пусть лучше перезвонят)
    private static final int MAX_CALLS_COUNT = 1_000;

    private static volatile boolean noMoreCalls;

    public static void main(String[] args) throws InterruptedException {
        final Queue<Call> callQueue = new LinkedBlockingQueue<>(MAX_CALLS_COUNT);

        final List<Thread> operatorsList = new ArrayList<>();
        for (int i = 0; i < OPERATORS_COUNT; i++) {
            final Thread operatorThread = new Thread(getOperatorAction(callQueue), "Оператор" + (i + 1));
            operatorsList.add(operatorThread);
            operatorThread.start();
        }

        final Thread atcThread = new Thread(getAtcAction(callQueue));
        atcThread.start();

        atcThread.join();
        operatorsList.forEach(thread -> {
            try {
                thread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
        System.out.println("Колл-центр закрывается");
    }

    @SuppressWarnings("BusyWait")
    private static Runnable getOperatorAction(Queue<Call> callQueue) {
        return () -> {
            while (true) {
                final Call call = callQueue.poll();
                try {
                    if (call == null) {
                        if (noMoreCalls) {
                            System.out.println(Thread.currentThread().getName() + " идет домой");
                            return;
                        }
                        Thread.sleep(TIME_BETWEEN_CHECK_CALL);
                    } else {
                        System.out.println(Thread.currentThread().getName() + " обрабатывает звонок " + call.getCallNumber());
                        Thread.sleep(TIME_TO_HANDLE_CALL);
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        };
    }

    private static Runnable getAtcAction(Queue<Call> callQueue) {
        return () -> {
            for (int i = 0; i < TIMES_TO_CALL; i++) {
                for (int j = 0; j < CALLS_COUNT; j++) {
                    final int callNumber = i * CALLS_COUNT + j;
                    if (callQueue.offer(new Call(callNumber))) {
                        System.out.println("Входящий звонок " + callNumber + " добавлен в очередь");
                    } else {
                        System.out.println("Входящий звонок " + callNumber + " проигнорирован");
                    }
                }
                try {
                    Thread.sleep(TIME_BETWEEN_CALLS);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            noMoreCalls = true;
        };
    }
}