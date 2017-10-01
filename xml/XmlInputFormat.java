package xml;

import java.nio.charset.StandardCharsets;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.DataOutputBuffer;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.InputSplit;
import org.apache.hadoop.mapreduce.RecordReader;
import org.apache.hadoop.mapreduce.TaskAttemptContext;
import org.apache.hadoop.mapreduce.lib.input.FileSplit;
import org.apache.hadoop.mapreduce.lib.input.TextInputFormat;
import org.apache.hadoop.conf.Configuration;  
import org.apache.hadoop.fs.FSDataInputStream;  
import org.apache.hadoop.fs.FileSystem;  
import org.apache.hadoop.fs.Path;  
import org.apache.hadoop.io.Text;  
import org.apache.hadoop.mapreduce.InputSplit;  
import org.apache.hadoop.mapreduce.RecordReader;  
import org.apache.hadoop.mapreduce.TaskAttemptContext;  
import org.apache.hadoop.mapreduce.lib.input.FileSplit;  
import org.apache.hadoop.util.LineReader;  
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;  
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IOUtils;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.compress.CompressionCodec;
import org.apache.hadoop.io.compress.CompressionCodecFactory;
import org.apache.hadoop.mapreduce.InputSplit;
import org.apache.hadoop.mapreduce.RecordReader;
import org.apache.hadoop.mapreduce.TaskAttemptContext;
import org.apache.hadoop.mapreduce.lib.input.CombineFileSplit;
import org.apache.hadoop.util.LineReader;

public class XmlInputFormat extends FileInputFormat<Text, Text> {  
  
    @Override  
    public RecordReader<Text, Text> createRecordReader(InputSplit split,  
        TaskAttemptContext context) throws IOException,  
        InterruptedException {  
        return new XmlRecordReader();  
    }  
  

public static   class XmlRecordReader extends RecordReader<Text ,Text>{  
  
    private LineReader lr ;  
    private long rlength;
    private String samplename="";
    private Text key = new Text();  
    private Text value = new Text();  
    private long start ;  
    private long end;  
    private long currentPos;  
    private Text line = new Text(); 
    private Path path;
    private Path dPath;
    private FileSystem fs;
	  
    @Override  
    public void initialize(InputSplit inputSplit, TaskAttemptContext cxt)  
            throws IOException, InterruptedException {  
        FileSplit split =(FileSplit) inputSplit;  
        Configuration conf = cxt.getConfiguration();  
        path = split.getPath();  
        fs = path.getFileSystem(conf);  
		
	boolean isCompressed =  findCodec(conf ,path);
	if(isCompressed)
	    codecWiseDecompress(cxt.getConfiguration());
     
        start =split.getStart();  
	if(isCompressed){
	    this.end = start + rlength;
  	} else {
            end = start + split.getLength(); 
            dPath =path;		
	}
	FSDataInputStream is = fs.open(dPath); 
        if(isCompressed)  fs.deleteOnExit(dPath);		
        lr = new LineReader(is,conf);  
        is.seek(start);  
		
        if(start!=0){  
            start += lr.readLine(new Text(),0,  (int)Math.min(Integer.MAX_VALUE, end-start));  
        }  
        currentPos = start;  
    }  
  
    
    @Override  
    public boolean nextKeyValue() throws IOException, InterruptedException {  

        if(currentPos > end){  
            return false;  
        }  
        currentPos += lr.readLine(line); 
		
        if(line.getLength()==0){  
            return false;  
        } 
        String symbol=line.toString().substring(0,2);
		
        if(symbol.equals("##")){ 
	    key.set("comment");
	    value.set("NA"); 
	    return true;
        } 
	symbol=line.toString().substring(0,6);//#CHROM
         
	if(symbol.equals("#CHROM")){ 		
	    key.set("header");
	    samplename=line.toString().split("\t")[9];
	    value.set(line);
	    return true;
        }
          
        // normal data records
        String [] words =null;
		
	words=line.toString().split("\t");  
      
        if(words.length<2){  
            System.err.println("line:"+line.toString()+".");  
            return false;  
        } 
		
        key.set(words[0]+"#"+words[1]+"#"+words[2]+"#"+words[3]);  
        value.set(words[4]+"HHH"+words[5]+"HHH"+words[6]+"HHH"+words[7]+"HHH"+samplename+"#"+words[8]+"HHH"+samplename+"#"+words[9]);  
        return true;  
          
    }  
  
    @Override  
    public Text getCurrentKey() throws IOException, InterruptedException {  
        return key;  
    }  
  
    @Override  
    public Text getCurrentValue() throws IOException, InterruptedException {  
        return value;  
    }  
  
    @Override  
    public float getProgress() throws IOException, InterruptedException {  
        if (start == end) {  
            return 0.0f;  
        } else {  
            return Math.min(1.0f, (currentPos - start) / (float) (end - start));  
        }  
    }  
  
    @Override  
    public void close() throws IOException {  
        // TODO Auto-generated method stub  
        lr.close();  
    } 

    private void codecWiseDecompress(Configuration conf) throws IOException{
		  
        CompressionCodecFactory factory = new CompressionCodecFactory(conf);
        CompressionCodec codec = factory.getCodec(path);
		    
        if (codec == null) {
	    System.err.println("No Codec Found For " + path);
	    System.exit(1);
	}
		    
	String outputUri = CompressionCodecFactory.removeSuffix(path.toString(), codec.getDefaultExtension());
	dPath = new Path(outputUri);
		    
	InputStream in = null;
	OutputStream out = null;
	fs = this.path.getFileSystem(conf);
	    
	try {
	    in = codec.createInputStream(fs.open(path));
	    out = fs.create(dPath);
	    IOUtils.copyBytes(in, out, conf);
    	} finally {
	    IOUtils.closeStream(in);
	    IOUtils.closeStream(out);
	    rlength = fs.getFileStatus(dPath).getLen();
	}
    }
	
    private boolean findCodec(Configuration conf, Path p){
		
	CompressionCodecFactory factory = new CompressionCodecFactory(conf);
	CompressionCodec codec = factory.getCodec(path);
	   
	if (codec == null) 
	    return false; 
	else 
	    return true;

    }	
      
}
}  
