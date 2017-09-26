/*********************************************************************************************
 *   CLASS Name:        MergeSample                                                          *
 *                                                                                           *
 *   CLASS Desc:        This class is used to merge VCF files by the Key. It is read by      *
 *                      newAPIHadoopFile API from Hadoop ( for performance requirement, we   *
 *                      did not use spark based file system). After it, we transform the RDD *
 *                      from TEXT to String format. Then we applied distinct() method to     *
 *                      remove the duplicates. So we obtained distinct RDDs right now.       *
 *                      Last, we applied groupByKey() method to merge the values of each     *
 *                      record by the key.                                                   *
 *                                                                                           *
 *                                                                                           * 
 *   AUTHORS:           Dianwei Han, Hui Li                                                  *
 *   CONTACT:           dianwei.han@mssm.edu, hui.li@mssm.edu                                *
 *   COPYRIGHT:         2017-2018 Reserved by Sema4, Chen team.                              * 
 *********************************************************************************************/


package mergesample;

import xml.XmlInputFormat;
import java.util.ArrayList;
import java.util.HashMap;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.SparkConf;
import org.apache.spark.api.java.function.VoidFunction;
import org.apache.spark.broadcast.Broadcast;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.*;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.conf.Configuration;
import scala.Tuple2;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.*;
import java.io.IOException;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.FileOutputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.StringWriter;
import java.net.URI;
import java.nio.file.Paths;

import java.nio.file.Files;
import java.nio.file.OpenOption;
import org.apache.hadoop.io.serializer.WritableSerialization;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.hadoop.mapred.*;
import org.apache.hadoop.io.compress.GzipCodec;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import org.apache.spark.api.java.function.FlatMapFunction;
import org.apache.spark.api.java.function.Function;
import org.apache.spark.api.java.function.Function2;
import org.apache.spark.api.java.function.PairFunction;
import org.apache.spark.api.java.JavaRDD;


import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.RemoteIterator;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IOUtils;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.compress.CompressionCodec;
import org.apache.hadoop.io.compress.CompressionCodecFactory;
import org.apache.hadoop.mapreduce.InputSplit;
import org.apache.hadoop.mapreduce.RecordReader;
import org.apache.hadoop.mapreduce.TaskAttemptContext;
import org.apache.hadoop.mapreduce.lib.input.CombineFileSplit;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.zip.GZIPInputStream;
import java.util.Set;
import java.util.Iterator;
import java.util.ListIterator;
import java.net.URI;
import scala.Tuple2;

public class MergeSample {

    public static String flatSampleList = null;
    public static ArrayList<String> rawSampleList = new ArrayList<String>();

    public static void setRawSampleList(String pathStr, Configuration config) {
        
        try {
           Path filesPath = new Path(pathStr);
           FileSystem fs = FileSystem.get(config);
           RemoteIterator<LocatedFileStatus> itr = fs.listFiles(filesPath, true);
           while (itr.hasNext()) {
                LocatedFileStatus f = itr.next();
               if (!f.isDirectory() && f.getPath().getName().endsWith("gz")) {
                   String cleanStr = f.getPath().getName().replace("superpanel_","").replace(".gvcf.gz","");
                   rawSampleList.add(cleanStr);
                   System.out.println("Header file "+ f.getPath().getName().toString());
               }
           }
        } catch (Exception e) {
          e.printStackTrace();
        }
    }
 
    public static ArrayList<String> getRawSampleList() {
       
        return rawSampleList;
    } 

    public static void setFlatSampleList() {

         for (int i = 0 ; i < rawSampleList.size() - 1; i++) {
             flatSampleList = flatSampleList + rawSampleList.get(i) + "#";
         }

         flatSampleList = flatSampleList + rawSampleList.get(rawSampleList.size()-1);
     
     }

    /**
     * Name:   getFlatSampleList
     * Desc:   return the flatSampleList 
     * Param:  null
     * return: String 
     */
    public static String getFlatSampleList() {
        return flatSampleList;
    }

    /**
     * Name:   setHeader
     * Desc:   set the samplelist field and the other fields.
     * Param:  String str
     * return: String
     */
    public static ArrayList<String> setHeader(String str) {
             ArrayList<String> rtnStr = new ArrayList<String>();
             String s = "#CHROM" + "\t" + "POS" + "\t" + "ID" + "\t" + "REF" + "\t" + "ALT" + "\t" + "QUAL" + "\t" + "FILTER" + "\t" + "INFO" + "\t" + "FORMAT" + "\t"; 

System.out.println("str " + str);
             String[] splitStr = str.split("#");
             for (int i =0;i<splitStr.length; i++) {
                 s = s + splitStr[i] + "\t";
             }
             rtnStr.add(s);
             return rtnStr;
    }

    public static void main(String[] args) throws IOException, InterruptedException {

        String inputPath  = args[0];
        String outputPath = args[1];
        String matrixFile = args[2];

        SparkConf sconf = new SparkConf().set("spark.serializer", "org.apache.spark.serializer.KryoSerializer").set("spark.executor.memory", "2g");//.set("spark.driver.host","localhost");
        
        JavaSparkContext sc = new JavaSparkContext(sconf);
        Configuration conf = new Configuration();
        conf.set("xmlinput.start", "<MedlineCitation");
        conf.set("xmlinput.end", "</MedlineCitation>");
        File file;

        setRawSampleList(inputPath, sc.hadoopConfiguration()); 
        System.out.println("files list : " + rawSampleList);

	JavaPairRDD<Text, Text> inputRDD = sc.newAPIHadoopFile(inputPath+"/*.gz", XmlInputFormat.class, Text.class, Text.class,conf);

        ArrayList<String> globalSampleList = getRawSampleList();
        final Broadcast<ArrayList<String>> broadGlobalSampleList = sc.broadcast(globalSampleList);

        System.out.println(" NUMofinputRDD " + inputRDD.count());

        JavaPairRDD<String, String> stringRDD=  inputRDD.mapToPair(new PairFunction<Tuple2<Text, Text>, String, String>() {
           public Tuple2<String, String> call(Tuple2<Text, Text> t) {

              return new Tuple2<String,String>(t._1.toString(), t._2.toString());

        }});
       
        System.out.println(" NUMofsringRDD " + stringRDD.count());
        System.out.println(" NUMdistinctRDD " + stringRDD.distinct().count());

      JavaPairRDD<String, String> distinctRDD = stringRDD.distinct();

      JavaPairRDD<String, Iterable<String>> groupRDD = distinctRDD.groupByKey();

        System.out.println(" NUMofgroup RDD " + groupRDD.count());


      JavaRDD<String>  resultRDD = groupRDD.map( 
          new Function<Tuple2<String, Iterable<String>>, String>() {
              public String call(Tuple2<String, Iterable<String>> x) {

                  //System.out.println(" key " + x._1 + " value " + x._2); 

                  String rtnStr = new String();
                  StringBuilder sb = new StringBuilder();
                  String[] keyStr = x._1.split("#");

                  // construct the key part of record
                  for (int i = 0; i < keyStr.length; i++) {
                       sb = sb.append(keyStr[i]);
                       sb = sb.append("\t");
                  }
                  
                  // construct the value part of record
                  ArrayList<String> templateFormat = new ArrayList<String>();
                  HashMap<String, String> mapLast = new HashMap<String, String>();
                  HashMap<String, String> mapFormat = new HashMap<String, String>();
                  ArrayList<String> altList = new ArrayList<String>();
                  ArrayList<Double> qualList = new ArrayList<Double>();
                  String filter = new String();
                  String info = new String();

                  for (String str : x._2) {
                      String[] rawField = str.split("HHH");
                      String alt = rawField[0];

                      if (altList.contains(alt) == false) {
                          altList.add(alt);
                      } 

                      String qual = rawField[1];
                      if (qual.equals(".") == false) {
                          qualList.add(Double.parseDouble(qual)); 
                      } 
                      filter = rawField[2];
                      info = rawField[3];

                      String[] twoComponent = rawField[4].split("#");
                     if ((twoComponent.length > 1)&&(twoComponent[0].length() > 2)) {
                          String[] fFormat = twoComponent[1].split(":");
                          for (int i = 0; i < fFormat.length; i++) {
                              if (templateFormat.contains(fFormat[i]) == false) {
                                  templateFormat.add(fFormat[i]);
                              } 
                          }
                          String[] sl = rawField[rawField.length -1].split("#");
                          if (sl.length > 1) {
                               mapLast.put(sl[0],sl[1]);
                          }

                          String[] elemLastField = sl[1].split(":"); 
                          for (int i = 0; i < fFormat.length; i++) {
                              mapFormat.put(twoComponent[0]+fFormat[i], elemLastField[i]);
                          }

                      }
                     
                  }  

                  // process the Alt field
                  for (int i=0; i<altList.size()-1; i++ ) {
                       //System.out.print(entry.getKey()+":");
                       sb = sb.append(altList.get(i) + "|");
                  }
                  sb = sb.append(altList.get(altList.size()-1) + "\t");
                   
                  // process the Qual field
                  double sum=0.0;
                  for ( Double dbl : qualList ) {
                      sum = sum + dbl;
                  }
                  sum = sum/qualList.size();
                  sb = sb.append(sum + "\t");
                  
                  sb = sb.append(filter + "\t");
                  sb = sb.append(info + "\t");

                  // process the Format field
                  if (templateFormat.size() > 5) {
                  System.out.println(" template size ["+ templateFormat.size() +"]");

                  for (int i=0; i<templateFormat.size()-1; i++ ) {
                       //System.out.print(entry.getKey()+":");
                       sb = sb.append(templateFormat.get(i) + ":");
                  }
                  sb = sb.append(templateFormat.get(templateFormat.size()-1) + "\t");
                  }

                                     

                  // process the last field
                  for (String str : broadGlobalSampleList.value()) {
                         if (mapLast.containsKey(str)) {

                              sb = sb.append(str);
                              sb = sb.append("#");
                              //sb = sb.append(map.get(str));
                              for (int i=0; i<templateFormat.size()-1; i++) {
                                  if (mapFormat.containsKey(str+templateFormat.get(i))==true) {
                                        sb=sb.append(mapFormat.get(str+templateFormat.get(i)));
                                        sb = sb.append(":");
                                  } else {
                                        sb=sb.append(".");
                                        sb = sb.append(":");
                                  }
                              }
                              if (mapFormat.containsKey(str+templateFormat.get(templateFormat.size()-1)) == true) {
                             
                                        sb=sb.append(mapFormat.get(str+templateFormat.get(templateFormat.size()-1)));
                               } else {
                                        sb = sb.append(".");
                               }
                              
                              sb = sb.append("\t");
                          }  else {
                              sb = sb.append(str);
                              sb = sb.append("#");
                              sb = sb.append("NA");
                              sb = sb.append("\t");
                          }
                  }
 

                  return sb.toString(); 
             } 
      });

      String flatsamplelist = getFlatSampleList();
      ArrayList<String> al = setHeader(flatsamplelist); 
      JavaRDD<String> header = sc.parallelize(al);
      header.union(resultRDD).saveAsTextFile(outputPath);

         
    }
}
         
