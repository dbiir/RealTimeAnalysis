package cn.edu.ruc.realtime.writer;

import cn.edu.ruc.realtime.utils.Output;
import org.apache.jasper.tagplugins.jstl.core.Out;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Queue;

/**
 * Created by Jelly on 7/1/16.
 */
public class FileWriter implements Writer {
    private BufferedWriter writer;

    public FileWriter(String path) {
        try {
            writer = Output.getBufferedWriter(path, 6*1024);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void write(Queue queue) {
        while (queue.peek() != null) {
            try {
                writer.write(String.valueOf(queue.poll())+"\n");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        try {
            writer.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}