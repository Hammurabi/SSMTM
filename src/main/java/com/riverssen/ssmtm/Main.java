//package com.riverssen.ssmtm;
//
//import java.net.UnknownHostException;
//import java.util.LinkedList;
//import java.util.Queue;
//import java.util.concurrent.ExecutorService;
//import java.util.concurrent.Executors;
//
//public class Main
//{
//    public static void main(String... args) throws UnknownHostException
//    {
//        TaskManager manager = new TCPTaskManager(10, (short) 5110);
//        ExecutorService service = Executors.newSingleThreadExecutor();
//        service.execute(manager);
//
//        System.out.println("HI");
//        manager.ForceConnect(new Peer("35.204.38.207"), 5110);
//        System.out.println("HIIII");
//        System.out.println(manager.GetConnected());
////        TaskManager manager = null;
////
////        Queue<String> queue = new LinkedList<>();
////
////        for (String command : args)
////            ((LinkedList<String>) queue).add(command);
////
////        while (queue.size() > 0)
////        {
////            switch (queue.poll())
////            {
////                case "-m":
////                case "-mode":
////                    if (queue.poll().toLowerCase().equals("tcp"))
////                        manager = new TCPTaskManager(Integer.parseInt(queue.poll()), Short.parseShort(queue.poll()));
////                    else
////                    {
////                        System.err.println("only tcp task manager allowed.");
////                        System.exit(0);
////                    }
////                    break;
////            }
////        }
////
////        ((TCPTaskManager) manager).run();
//    }
//}
