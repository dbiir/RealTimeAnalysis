package cn.edu.ruc.realtime.threads;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.parquet.column.ParquetProperties;
import org.apache.parquet.example.data.Group;
import org.apache.parquet.example.data.simple.SimpleGroupFactory;
import org.apache.parquet.hadoop.ParquetWriter;
import org.apache.parquet.hadoop.example.GroupWriteSupport;
import org.apache.parquet.hadoop.metadata.CompressionCodecName;
import org.apache.parquet.schema.MessageType;
import org.apache.parquet.schema.MessageTypeParser;

import java.io.IOException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * !!No Use!! Just for TEST
 * Created by Jelly on 6/29/16.
 */
public class ParquetWriterThread extends WriterThread {
    private BlockingQueue<ConsumerRecord> queue;
    private Collection<ConsumerRecord> list = new LinkedList();
    private AtomicBoolean isReadyToStop = new AtomicBoolean(false);

    public ParquetWriterThread(BlockingQueue queue) {
        this.queue = queue;
    }

    public void run() {
        Configuration conf = new Configuration();
        int blockSize = 1 * 1024;
        int pageSize = 1 * 1024;
        int dictionaryPageSize = 512;
        boolean enableDictionary = false;
        boolean validating = false;
        Path basePath = new Path("file:///Users/Jelly/Developer/test");
        MessageType schema = MessageTypeParser.parseMessageType("message test {" +
                "required binary id; " +
                "required binary content; " +
                "required int64 int64_field; " +
                "}");
        GroupWriteSupport writeSupport = new GroupWriteSupport();
        writeSupport.setSchema(schema, conf);
        SimpleGroupFactory groupFactory = new SimpleGroupFactory(schema);

        try {
            ParquetWriter<Group> parquetWriter = new ParquetWriter(
                    basePath,
                    writeSupport,
                    CompressionCodecName.UNCOMPRESSED,
                    blockSize, pageSize, dictionaryPageSize,
                    enableDictionary,
                    validating,
                    ParquetProperties.WriterVersion.PARQUET_2_0,
                    conf);
            for (int i = 0; i < 50000; i++) {
                parquetWriter.write(groupFactory.newGroup()
                        .append("id", "10")
                        .append("content", "test" + i)
                        .append("int64_field", Long.valueOf(i)));
            }
            parquetWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String getName() {
        return "Writer";
    }

    @Override
    public void setReadyToStop() {
        isReadyToStop.set(true);
    }

    @Override
    public boolean readyToStop() {
        return isReadyToStop.get();
    }
}
