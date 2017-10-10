package dbscan;

import au.com.bytecode.opencsv.CSVWriter;
import xml.XmlInputFormat;

import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.SparkConf;
import org.apache.spark.api.java.function.VoidFunction;
import org.apache.spark.broadcast.Broadcast;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.conf.Configuration;
import scala.Tuple2;

import java.io.IOException;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.FileOutputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.StringWriter;
import java.net.URI;
import java.nio.file.Paths;
import java.nio.file.Path;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.util.HashMap;
import java.util.ArrayList;
import org.apache.spark.api.java.JavaRDD;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.apache.spark.api.java.function.Function;
import org.apache.spark.api.java.function.FlatMapFunction;
import org.apache.spark.api.java.function.Function;
import org.apache.spark.api.java.function.Function2;
import org.apache.spark.api.java.function.PairFunction;
import org.apache.spark.api.java.JavaRDD;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.NamedNodeMap;

public class XMLTextMining {

    public static void main(String[] args) throws IOException, InterruptedException {

        String inputPath  = args[0];
        String outputPath = args[1];

        JavaSparkContext sc = new JavaSparkContext();
         
        Configuration conf = new Configuration();

        conf.set("xmlinput.start", "<ClinVarSet");
        conf.set("xmlinput.end", "</ClinVarSet>");
        JavaPairRDD<LongWritable, Text> inputRDD = sc.newAPIHadoopFile(inputPath, XmlInputFormat.class, LongWritable.class, Text.class,conf);
        
        //System.out.println("-----------------------------------");
        //System.out.println(inputRDD.count());
        //System.out.println("-----------------------------------");
        
        final Broadcast<String> broadoutputDir = sc.broadcast(outputPath); 
        

        JavaRDD<ArrayList<Tuple2<String, String>>> hRDD = inputRDD.map(
          new  Function<Tuple2<LongWritable,Text>, ArrayList<Tuple2<String, String>>>() { 
               public ArrayList<Tuple2<String, String>>  call(Tuple2<LongWritable,Text> f) {
           
         ArrayList<Tuple2<String,String>> iter = new ArrayList<Tuple2<String, String>>();
          try{
            
            int ref_sum = 0;


            InputStream inputStr = new ByteArrayInputStream(f._2.getBytes());
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbFactory.newDocumentBuilder();
            Document doc = db.parse(inputStr);
            doc.getDocumentElement().normalize();
           
            //BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream("test", ));
            FileOutputStream out = new FileOutputStream((new File("ClinVarParsedResult.csv")), true);

            StringWriter sw = new StringWriter();  
            CSVWriter csv = new CSVWriter(sw);
            Configuration newConf = new Configuration();
            Path p = Paths.get(broadoutputDir.value());

            long start_time = System.currentTimeMillis();


            NodeList nrefClinVar = doc.getElementsByTagName("ReferenceClinVarAssertion");
    
                Node node = nrefClinVar.item(0);
                     System.out.println("------------------------------------ClinVar Parsing RESULT From XML------------------------------------");
                     System.out.println("Title: " + doc.getElementsByTagName("Title").item(0).getTextContent());
                 //    System.out.println("|-- ReferenceClinVarAssertion");
                     NamedNodeMap atts = node.getAttributes();
                     for (int i = 0; i < atts.getLength(); i ++ ) {
                          Node att = atts.item(i);
                          if (att.getNodeName().equals("DateCreated")) {
                             // System.out.println("|    |-- DateCreated " + att.getTextContent());
                             Tuple2<String, String> t2 = new Tuple2<String, String>("ReferenceClinvarAssertion.DateCreated", att.getTextContent());
                                                iter.add(t2);
                          }
                          if (att.getNodeName().equals("DateLastUpdated")) {
                              //System.out.println("|    |-- DateLastUpdated " + att.getTextContent());
                              Tuple2<String, String> t2 = new Tuple2<String, String>("ReferenceClinvarAssertion.DateLastUpdated", att.getTextContent());
                                                iter.add(t2);
                          }
                          if (att.getNodeName().equals("ID")) {
                            //  System.out.println("|    |-- ID " + att.getTextContent());
                            Tuple2<String, String> t2 = new Tuple2<String, String>("ReferenceClinvarAssertion.ID", att.getTextContent());
                                                iter.add(t2);
                          }
                     }
                     Element e = (Element) node;
                     String assertion = e.getElementsByTagName("Assertion").item(0).getTextContent();
                //     System.out.println("|    |-- Assertion: " + assertion);



                     NodeList refClinVar_attributeSet = e.getElementsByTagName("AttributeSet");
                     if (refClinVar_attributeSet.getLength() == 0) {
                          System.out.println("|    |-- AttributeSet:  null");
                     }
                     
                     NodeList refClinVar_citation = e.getElementsByTagName("Citation");
                     if (refClinVar_citation.getLength() == 0) {
                          System.out.println("|    |-- Citation:  null");
                          //System.out.println("ReferenceClinVarAssertion.Citation: null");
                     }
                     String refClinVar_ClinVarAccession = e.getElementsByTagName("ClinVarAccession").item(0).getTextContent();
                     System.out.println("|    |-- ClinVarAccession: "+  refClinVar_ClinVarAccession ); 

                     // clinical_significance
                     Node refClinVar_ClinicalSignificance = e.getElementsByTagName("ClinicalSignificance").item(0);


                     //System.out.println("|    |-- ClinicalSignificance " ); 

                     NamedNodeMap csatts = refClinVar_ClinicalSignificance.getAttributes();
                     if(csatts.item(0).getNodeName().equals("DateLastEvaluated")) {
                         //  System.out.println("|    |    |-- DateLastEvaluated: " + csatts.item(0).getTextContent());
   Tuple2<String, String> t2 = new Tuple2<String, String>("ReferenceClinvarAssertion.ClinicalSignificance.DateLastEvaluated", csatts.item(0).getTextContent());
                                                iter.add(t2);
                     }
 
                     if (! refClinVar_ClinicalSignificance.hasChildNodes()) {
                           System.out.println(" significance no child ");
                     }
                     NodeList list = refClinVar_ClinicalSignificance.getChildNodes();
                     for (int i =0 ; i< list.getLength(); i++) {
                          Node subnode = list.item(i);
                          if (subnode.getNodeName().equals("Description")) {
                              // System.out.println("|    |    |-- Description: " + subnode.getTextContent());
   Tuple2<String, String> t2 = new Tuple2<String, String>("ReferenceClinvarAssertion.ClinicalSignificance.Description", subnode.getTextContent());
                                                iter.add(t2);
                          }
                          if (subnode.getNodeName().equals("ReviewStatus")) {
                               //System.out.println("|    |    |-- ReviewStatus: " + subnode.getTextContent());
   Tuple2<String, String> t2 = new Tuple2<String, String>("ReferenceClinvarAssertion.ClinicalSignificance.ReviewStatus", subnode.getTextContent());
                                                iter.add(t2);
                          }

                         
                     }

                     
                     //GenotypeSet

                     Node refCV_GenotypeSet = e.getElementsByTagName("GenotypeSet").item(0);


                  //   System.out.println("|    |-- GenotypeSet:");
                     NamedNodeMap genoatts = refCV_GenotypeSet.getAttributes();
                     if(genoatts.item(0).getNodeName().equals("ID")) {
                           System.out.println("|    |    |-- ID: " + genoatts.item(0).getTextContent());
                     }   
 
                     if (! refCV_GenotypeSet.hasChildNodes()) {
                           System.out.println(" genotype no child "); 
                     }   
                     NodeList genolist = refCV_GenotypeSet.getChildNodes();

                     if (genolist.getLength() > 1) {
                       ref_sum = ref_sum + 1000;
     //  System.out.println(" ref_sum " + ref_sum);
                     }
                     //System.out.println("genolist Length  " + genolist.getLength());

                     for (int i =0 ; i< genolist.getLength(); i++) {
                          Node subnode = genolist.item(i);
                          //System.out.println("i  [" +  i + "] " + "Name " +  subnode.getNodeName());

                          if (subnode.getNodeName().equals("AttributeSet")) {
                               System.out.println("|    |    |-- AttributeSet: ");// + subnode.getTextContent());
                               NodeList attlist = subnode.getChildNodes();
                               for (int k=0; k<attlist.getLength(); k++) {
                                      Node att = attlist.item(k);
                                      if (att.getNodeName().equals("Attribute")) {
                                         Tuple2<String, String> t2 = new Tuple2<String, String>("ReferenceClinvarAssertion.GenotypeSet.AttributeSet.Attribute", att.getTextContent());
                                         iter.add(t2);
                                               System.out.println("|    |    |    | -- Attribute "  + att.getTextContent());
                                       }
                                }

                          }   

                          if (subnode.getNodeName().equals("Name")) {
                               System.out.println("|    |    |-- Name: ");// + subnode.getTextContent());
                               NodeList attlist = subnode.getChildNodes();
                               for (int k=0; k<attlist.getLength(); k++) {
                                      Node att = attlist.item(k);
                                      if (att.getNodeName().equals("ElementValue")) {
                                         Tuple2<String, String> t2 = new Tuple2<String, String>("ReferenceClinvarAssertion.GenotypeSet.Name.ElementValue", att.getTextContent());
                                         iter.add(t2);
                                               System.out.println("|    |    |    | -- ElementValue "  + att.getTextContent());
                                       }
                                }

                          }   

                          // rcv__genotypeset__measureset
                          if (subnode.getNodeName().equals("MeasureSet")) {
                           
                               ref_sum = ref_sum + 200;

                               System.out.println("|    |    |-- MeasureSet: ");
                               NamedNodeMap msatts = subnode.getAttributes();
                               for (int j = 0; j < msatts.getLength(); j ++ ) {
                                  Node att = msatts.item(j);
                                  if (att.getNodeName().equals("ID")) {
                                      System.out.println("|    |    |    |-- ID " + att.getTextContent());
                                   }
                                  if (att.getNodeName().equals("Type")) {
                                      System.out.println("|    |    |    |-- Type " + att.getTextContent());
                                   }
                                }

                               
                               NodeList mslist = subnode.getChildNodes();
                               for (int j=0; j < mslist.getLength(); j++) {
                                  Node ssubnode = mslist.item(j);
              
                                  if (ssubnode.getNodeName().equals("Name")) {

                                      System.out.println("|    |    |    |-- Name ");//  + ssubnode.getTextContent());

                                      NodeList attlist = ssubnode.getChildNodes();
                                      for (int k=0; k<attlist.getLength(); k++) {
                                         Node att = attlist.item(k);
                                         if (att.getNodeName().equals("ElementValue")) {
                                         Tuple2<String, String> t2 = new Tuple2<String, String>("ReferenceClinvarAssertion.GenotypeSet.MeasureSet.Name.ElementValue", att.getTextContent());
                                         iter.add(t2);
                                               System.out.println("|    |    |    |    |    |-- ElementValue "  + att.getTextContent());
                                         }

                                         if (att.getNodeName().equals("XRef")) {
                                               System.out.println("|    |    |    |    |    |-- XRef "  + att.getTextContent());
                                         }
                                      }
                                  }
                                      
                                  if (ssubnode.getNodeName().equals("AttributeSet")) {
                                      System.out.println("|    |    |    | -- AttributeSet ");//  + ssubnode.getTextContent());
                                      NodeList attlist = ssubnode.getChildNodes();
                                      for (int k=0; k<attlist.getLength(); k++) {
                                           Node att = attlist.item(k);
                                           if (att.getNodeName().equals("Attribute")) { 
                                         Tuple2<String, String> t2 = new Tuple2<String, String>("ReferenceClinvarAssertion.GenotypeSet.MeasureSet.AttributeSet.Attribute", att.getTextContent());
                                         iter.add(t2);
                                               System.out.println("|    |    |    |    |-- Attribute "  + att.getTextContent());
                                           }
                                      }
                                      
                                  }

                                  if (ssubnode.getNodeName().equals("Measure")) {

                               ref_sum = ref_sum + 200;

                                      System.out.println("|    |    |    |-- Measure " );

                                      NamedNodeMap matts = ssubnode.getAttributes();
                                      for (int k = 0; k < matts.getLength(); k ++ ) {
                                         Node att = matts.item(k);
                                         if (att.getNodeName().equals("ID")) {
                                             System.out.println("|    |    |    |    |-- ID " + att.getTextContent());
                                         }
                                         if (att.getNodeName().equals("Type")) {
                                             System.out.println("|    |    |    |    |-- Type " + att.getTextContent());
                                         }
                                      }

                                      NodeList ssubnodelist = ssubnode.getChildNodes();

                                      for (int k = 0; k < ssubnodelist.getLength(); k++) {
                                         Node sssubnode = ssubnodelist.item(k);

                                         // ref_genotypset_mset_measure_name
                                         if (sssubnode.getNodeName().equals("Name")) {

                                             System.out.println("|    |    |    |    |-- Name ");// + sssubnode.getTextContent());

                                             NodeList attlist = sssubnode.getChildNodes();
                                             for (int m=0; m<attlist.getLength(); m++) {
                                                Node att = attlist.item(m);
                                                if (att.getNodeName().equals("ElementValue")) {
                                         Tuple2<String, String> t2 = new Tuple2<String, String>("ReferenceClinvarAssertion.GenotypeSet.MeasureSet.Measure.Name.ElementValue", att.getTextContent());
                                         iter.add(t2);
                                                    System.out.println("|    |    |    |    |    |-- ElementValue "  + att.getTextContent());
                                                }

                                                if (att.getNodeName().equals("XRef")) {
                                                     System.out.println("|    |    |    |    |    |-- XRef "  + att.getTextContent());
                                                }
                                             }

                                         }
                                         // ref_genotypeset_mset_measure_CytogeneticLocation
                                         if (sssubnode.getNodeName().equals("CytogeneticLocation")) {
                                         Tuple2<String, String> t2 = new Tuple2<String, String>("ReferenceClinvarAssertion.GenotypeSet.MeasureSet.Measure.CytogeneticLocation", sssubnode.getTextContent());
                                         iter.add(t2);
                                            System.out.println("|    |    |    |    |-- CytogeneticLocation " + sssubnode.getTextContent());
                                         }

                                         // ref_genotypeset_mset_measure_sequenceLocation
                                         if (sssubnode.getNodeName().equals("SequenceLocation")) {
                                             System.out.println("|    |    |    |    |-- SequenceLocation ");// + sssubnode.getTextContent());
                                             NamedNodeMap sq = sssubnode.getAttributes();
                                             for (int m = 0; m < sq.getLength(); m++) {
                                                  Node att = sq.item(m);
                                                  if (att.getNodeName().equals("Assembly")) {
                                         Tuple2<String, String> t2 = new Tuple2<String, String>("ReferenceClinvarAssertion.GenotypeSet.MeasureSet.Measure.sequenceLocation.Assembly", att.getTextContent());
                                         iter.add(t2);
                                                      System.out.println("|    |    |    |    |    |-- Assembly: " + att.getTextContent());// + sssubnode.getTextContent());
                                                  }
                                                  if (att.getNodeName().equals("AssemblyAccessionVersion")) {
                                         Tuple2<String, String> t2 = new Tuple2<String, String>("ReferenceClinvarAssertion.GenotypeSet.MeasureSet.Measure.sequenceLocation.AssemblyAccess", att.getTextContent());
                                         iter.add(t2);
                                                      System.out.println("|    |    |    |    |    |-- AssemblyAccessVersion: " + att.getTextContent());// + sssubnode.getTextContent());
                                                  }
                                                  if (att.getNodeName().equals("AssemblyStatus")) {
                                         Tuple2<String, String> t2 = new Tuple2<String, String>("ReferenceClinvarAssertion.GenotypeSet.MeasureSet.Measure.sequenceLocation.AssemblyStatus", att.getTextContent());
                                         iter.add(t2);
                                                      System.out.println("|    |    |    |    |    |-- AssemblyStatus: " + att.getTextContent());// + sssubnode.getTextContent());
                                                  }
                                                  if (att.getNodeName().equals("Chr")) {
                                         Tuple2<String, String> t2 = new Tuple2<String, String>("ReferenceClinvarAssertion.GenotypeSet.MeasureSet.Measure.sequenceLocation.Chr", att.getTextContent());
                                         iter.add(t2);
                                                      System.out.println("|    |    |    |    |    |-- Chr: " + att.getTextContent());// + sssubnode.getTextContent());
                                                  }
                                                  if (att.getNodeName().equals("Accession")) {
                                         Tuple2<String, String> t2 = new Tuple2<String, String>("ReferenceClinvarAssertion.GenotypeSet.MeasureSet.Measure.sequenceLocation.Accession", att.getTextContent());
                                         iter.add(t2);
                                                      System.out.println("|    |    |    |    |    |-- Accession: " + att.getTextContent());// + sssubnode.getTextContent());
                                                  }
                                                  if (att.getNodeName().equals("start")) {
                                         Tuple2<String, String> t2 = new Tuple2<String, String>("ReferenceClinvarAssertion.GenotypeSet.MeasureSet.Measure.sequenceLocation.start", att.getTextContent());
                                         iter.add(t2);
                                                      System.out.println("|    |    |    |    |    |-- start: " + att.getTextContent());// + sssubnode.getTextContent());
                                                  }
                                                  if (att.getNodeName().equals("stop")) {
                                         Tuple2<String, String> t2 = new Tuple2<String, String>("ReferenceClinvarAssertion.GenotypeSet.MeasureSet.Measure.sequenceLocation.stop", att.getTextContent());
                                         iter.add(t2);
                                                      System.out.println("|    |    |    |    |    |-- stop: " + att.getTextContent());// + sssubnode.getTextContent());
                                                  }
                                                  if (att.getNodeName().equals("display_start")) {
                                         Tuple2<String, String> t2 = new Tuple2<String, String>("ReferenceClinvarAssertion.GenotypeSet.MeasureSet.Measure.sequenceLocation.display_start", att.getTextContent());
                                         iter.add(t2);
                                                      System.out.println("|    |    |    |    |    |-- display_start: " + att.getTextContent());// + sssubnode.getTextContent());
                                                  }
                                                  if (att.getNodeName().equals("variantLength")) {
                                         Tuple2<String, String> t2 = new Tuple2<String, String>("ReferenceClinvarAssertion.GenotypeSet.MeasureSet.Measure.sequenceLocation.variantLength", att.getTextContent());
                                         iter.add(t2);
                                                      System.out.println("|    |    |    |    |    |-- variantLength: " + att.getTextContent());// + sssubnode.getTextContent());
                                                  }
                                                  if (att.getNodeName().equals("referenceAllele")) {
                                         Tuple2<String, String> t2 = new Tuple2<String, String>("ReferenceClinvarAssertion.GenotypeSet.MeasureSet.Measure.sequenceLocation.referenceAllele", att.getTextContent());
                                         iter.add(t2);
                                                      System.out.println("|    |    |    |    |    |-- referenceAllele: " + att.getTextContent());// + sssubnode.getTextContent());
                                                  }
                                                  if (att.getNodeName().equals("alternateAllele")) {
                                         Tuple2<String, String> t2 = new Tuple2<String, String>("ReferenceClinvarAssertion.GenotypeSet.MeasureSet.Measure.sequenceLocation.alternateAllele", att.getTextContent());
                                         iter.add(t2);
                                                      System.out.println("|    |    |    |    |    |-- alternateAllele: " + att.getTextContent());// + sssubnode.getTextContent());
                                                  }
                                                  if (att.getNodeName().equals("positionVCF")) {
                                         Tuple2<String, String> t2 = new Tuple2<String, String>("ReferenceClinvarAssertion.GenotypeSet.MeasureSet.Measure.sequenceLocation.positionVCF", att.getTextContent());
                                         iter.add(t2);
                                                      System.out.println("|    |    |    |    |    |-- positionVCF: " + att.getTextContent());// + sssubnode.getTextContent());
                                                  }
                                                  if (att.getNodeName().equals("referenceAlleleVCF")) {
                                         Tuple2<String, String> t2 = new Tuple2<String, String>("ReferenceClinvarAssertion.GenotypeSet.MeasureSet.Measure.sequenceLocation.referenceAlleVCF", att.getTextContent());
                                         iter.add(t2);
                                                      System.out.println("|    |    |    |    |    |-- referenceAlleleVCF: " + att.getTextContent());// + sssubnode.getTextContent());
                                                  }
                                                  if (att.getNodeName().equals("alternateAlleleVCF")) {
                                         Tuple2<String, String> t2 = new Tuple2<String, String>("ReferenceClinvarAssertion.GenotypeSet.MeasureSet.Measure.sequenceLocation.alternateAlleleVCF", att.getTextContent());
                                         iter.add(t2);
                                                      System.out.println("|    |    |    |    |    |-- alternateAlleleVCF: " + att.getTextContent());// + sssubnode.getTextContent());
                                                  }
                                                  if (att.getNodeName().equals("Strand")) {
                                         Tuple2<String, String> t2 = new Tuple2<String, String>("ReferenceClinvarAssertion.GenotypeSet.MeasureSet.Measure.sequenceLocation.Strand", att.getTextContent());
                                         iter.add(t2);
                                                      System.out.println("|    |    |    |    |    |-- Strand: " + att.getTextContent());// + sssubnode.getTextContent());
                                                  }
                                              }
                                            
                                         }

                                         // ref_genotypset_mset_measure_attributeSet
                                         if (sssubnode.getNodeName().equals("AttributeSet")) {
                                            System.out.println("|    |    |    |    |-- AttributeSet ");//  + ssubnode.getTextContent());
                                            NodeList attlist = sssubnode.getChildNodes();
                                            for (int m=0; m<attlist.getLength(); m++) {
                                                Node att = attlist.item(m);

                                                if (att.getNodeName().equals("Attribute")) { 
                                                    Tuple2<String, String> t2 = new Tuple2<String, String>("ReferenceClinvarAssertion.GenotypeSet.MeasureSet.Measure.AttributeSet.Attribute", att.getTextContent());
                                                    iter.add(t2);
                                                    System.out.println("|    |    |    |    |    |-- Attribute "  + att.getTextContent());
                                                }
                                                if (att.getNodeName().equals("XRef")) { 
                                                    System.out.println("|    |    |    |    |    |-- XRef ");//  + att.getTextContent());
                                                    NamedNodeMap mattsa = att.getAttributes();
                                                    for (int h = 0; h < mattsa.getLength(); h++) {
                                                       Node attl= mattsa.item(h);
                                                       if (attl.getNodeName().equals("ID")) {
                                                             Tuple2<String, String> t2 = new Tuple2<String, String>("ReferenceClinvarAssertion.GenotypeSet.MeasureSet.Measure.XRef.ID", attl.getTextContent());
                                                             iter.add(t2);
                                                            System.out.println("|    |    |    |    |    |    |-- ID " + attl.getTextContent());//  + att.getTextContent());
                                                       }
                                                       if (attl.getNodeName().equals("DB")) {
                                                            Tuple2<String, String> t2 = new Tuple2<String, String>("ReferenceClinvarAssertion.GenotypeSet.MeasureSet.Measure.XRef.DB", attl.getTextContent());
                                                            iter.add(t2);
                                                            System.out.println("|    |    |    |    |    |    |-- DB " + attl.getTextContent());//  + att.getTextContent());
                                                       }
                                                    }
                                                }
                                            }
                                      
                                          } // end of ref_geno_mset_attributeset

                                         // ref_genotypset_mset_measure_measurerelationship
                                         if (sssubnode.getNodeName().equals("MeasureRelationship")) {

                               ref_sum = ref_sum + 200;

                                            System.out.println("|    |    |    |    |-- MeasureRelationship ");//  + ssubnode.getTextContent());
                                            NamedNodeMap mratt = sssubnode.getAttributes();
                                            if (mratt.item(0).getNodeName().equals("Type")) {
                                         Tuple2<String, String> t2 = new Tuple2<String, String>("ReferenceClinvarAssertion.GenotypeSet.MeasureSet.measure.mrelationship.Type", mratt.item(0).getTextContent());
                                         iter.add(t2);
                                                System.out.println("|    |    |    |    |    |-- Type: " + mratt.item(0).getTextContent() );//  + ssubnode.getTextContent());
                                            }
  
    
                                            NodeList attlist = sssubnode.getChildNodes();
                                            for (int m=0; m<attlist.getLength(); m++) {
                                                Node att = attlist.item(m);

                                                 if (att.getNodeName().equals("Name")) {
                                                     System.out.println("|    |    |    |    |    |-- Name ");//  + ssubnode.getTextContent());

                                                     NodeList attlistname = att.getChildNodes();
                                                     for (int h=0; h<attlistname.getLength(); h++) {
                                                        Node attname = attlistname.item(h);
                                                        if (attname.getNodeName().equals("ElementValue")) {
                                                     Tuple2<String, String> t2 = new Tuple2<String, String>("ReferenceClinvarAssertion.GenotypeSet.MeasureSet.measure.mrelationship.Name.ElementValue", attname.getTextContent());
                                                     iter.add(t2);
                                                            System.out.println("|    |    |    |    |    |    |-- ElementValue "  + attname.getTextContent());
                                                         }

                                                     }
                                                 }
                                                 if (att.getNodeName().equals("Symbol")) {
                                                     System.out.println("|    |    |    |    |    |-- Symbol ");//  + ssubnode.getTextContent());

                                                     NodeList attlistname = att.getChildNodes();
                                                     for (int h=0; h<attlistname.getLength(); h++) {
                                                        Node attname = attlistname.item(h);
                                                        if (attname.getNodeName().equals("ElementValue")) {
                                                     Tuple2<String, String> t2 = new Tuple2<String, String>("ReferenceClinvarAssertion.GenotypeSet.MeasureSet.measure.mrelationship.Symbol.ElementValue", attname.getTextContent());
                                                     iter.add(t2);
                                                            System.out.println("|    |    |    |    |    |    |-- ElementValue "  + attname.getTextContent());
                                                         }

                                                     }
                                                 }


                                                 if (att.getNodeName().equals("SequenceLocation")) {
                                                      System.out.println("|    |    |    |    |    |-- SequenceLocation ");// + sssubnode.getTextContent());
                                                      NamedNodeMap sq = att.getAttributes();
                                             	      for (int h = 0; h < sq.getLength(); h++) {
                                                  	  Node ttt = sq.item(h);
                                                  	  if (ttt.getNodeName().equals("Assembly")) {
                                                     Tuple2<String, String> t2 = new Tuple2<String, String>("ReferenceClinvarAssertion.GenotypeSet.MeasureSet.measure.mrelationship.sequenceL.Assembly", ttt.getTextContent());
                                                     iter.add(t2);
                                                            System.out.println("|    |    |    |    |    |    |-- Assembly: " + ttt.getTextContent());// + sssubnode.getTextContent());
                                                          }   
                                                          if (ttt.getNodeName().equals("AssemblyAccessionVersion")) {
                                                     Tuple2<String, String> t2 = new Tuple2<String, String>("ReferenceClinvarAssertion.GenotypeSet.MeasureSet.measure.mrelationship.sequenceL.AssemblyAccess", ttt.getTextContent());
                                                     iter.add(t2);
                                                             System.out.println("|    |    |    |    |    |    |-- AssemblyAccessVersion: " + ttt.getTextContent());// + sssubnode.getTextContent());
                                                          }   
                                                          if (ttt.getNodeName().equals("AssemblyStatus")) {
                                                     Tuple2<String, String> t2 = new Tuple2<String, String>("ReferenceClinvarAssertion.GenotypeSet.MeasureSet.measure.mrelationship.sequenceL.AssemblyStatus", ttt.getTextContent());
                                                     iter.add(t2);
                                                             System.out.println("|    |    |    |    |    |    |-- AssemblyStatus: " + ttt.getTextContent());// + sssubnode.getTextContent());
                                                          }   
                                                          if (ttt.getNodeName().equals("Chr")) {
                                                     Tuple2<String, String> t2 = new Tuple2<String, String>("ReferenceClinvarAssertion.GenotypeSet.MeasureSet.measure.mrelationship.sequenceL.Chr", ttt.getTextContent());
                                                     iter.add(t2);
                                                             System.out.println("|    |    |    |    |    |    |-- Chr: " + ttt.getTextContent());// + sssubnode.getTextContent());
                                                          }   
                                                          if (ttt.getNodeName().equals("Accession")) {
                                                     Tuple2<String, String> t2 = new Tuple2<String, String>("ReferenceClinvarAssertion.GenotypeSet.MeasureSet.measure.mrelationship.sequenceL.Accession", ttt.getTextContent());
                                                     iter.add(t2);
                                                             System.out.println("|    |    |    |    |    |    |-- Accession: " + ttt.getTextContent());// + sssubnode.getTextContent());
                                                          }   
                                                          if (ttt.getNodeName().equals("start")) {
                                                     Tuple2<String, String> t2 = new Tuple2<String, String>("ReferenceClinvarAssertion.GenotypeSet.MeasureSet.measure.mrelationship.sequenceL.start", ttt.getTextContent());
                                                     iter.add(t2);
                                                              System.out.println("|    |    |    |    |    |    |-- start: " + ttt.getTextContent());// + sssubnode.getTextContent());
                                                          }   
                                                          if (ttt.getNodeName().equals("stop")) {
                                                     Tuple2<String, String> t2 = new Tuple2<String, String>("ReferenceClinvarAssertion.GenotypeSet.MeasureSet.measure.mrelationship.sequenceL.stop", ttt.getTextContent());
                                                     iter.add(t2);
                                                              System.out.println("|    |    |    |    |    |    |-- stop: " + ttt.getTextContent());// + sssubnode.getTextContent());
                                                          }   
                                                          if (ttt.getNodeName().equals("display_start")) {
                                                     Tuple2<String, String> t2 = new Tuple2<String, String>("ReferenceClinvarAssertion.GenotypeSet.MeasureSet.measure.mrelationship.sequenceL.display_start", ttt.getTextContent());
                                                     iter.add(t2);
                                                              System.out.println("|    |    |    |    |    |    |-- display_start: " + ttt.getTextContent());// + sssubnode.getTextContent());
                                                          }   
                                                          if (ttt.getNodeName().equals("variantLength")) {
                                                     Tuple2<String, String> t2 = new Tuple2<String, String>("ReferenceClinvarAssertion.GenotypeSet.MeasureSet.measure.mrelationship.sequenceL.variantLength", ttt.getTextContent());
                                                     iter.add(t2);
                                                              System.out.println("|    |    |    |    |    |    |-- variantLength: " + ttt.getTextContent());// + sssubnode.getTextContent());
                                                          }   
                                                          if (ttt.getNodeName().equals("referenceAllele")) {
                                                     Tuple2<String, String> t2 = new Tuple2<String, String>("ReferenceClinvarAssertion.GenotypeSet.MeasureSet.measure.mrelationship.sequenceL.referenceAllele", ttt.getTextContent());
                                                     iter.add(t2);
                                                              System.out.println("|    |    |    |    |    |    |-- referenceAllele: " + ttt.getTextContent());// + sssubnode.getTextContent());
                                                          }   
                                                          if (ttt.getNodeName().equals("alternateAllele")) {
                                                     Tuple2<String, String> t2 = new Tuple2<String, String>("ReferenceClinvarAssertion.GenotypeSet.MeasureSet.measure.mrelationship.sequenceL.alternateAllele", ttt.getTextContent());
                                                     iter.add(t2);
                                                              System.out.println("|    |    |    |    |    |    |-- alternateAllele: " + ttt.getTextContent());// + sssubnode.getTextContent());
                                                          }   
                                                          if (ttt.getNodeName().equals("positionVCF")) {
                                                     Tuple2<String, String> t2 = new Tuple2<String, String>("ReferenceClinvarAssertion.GenotypeSet.MeasureSet.measure.mrelationship.sequenceL.positionVCFAllele", ttt.getTextContent());
                                                     iter.add(t2);
                                                              System.out.println("|    |    |    |    |    |    |-- positionVCF: " + ttt.getTextContent());// + sssubnode.getTextContent());
                                                          }   
                                                          if (ttt.getNodeName().equals("referenceAlleVCF")) {
                                                     Tuple2<String, String> t2 = new Tuple2<String, String>("ReferenceClinvarAssertion.GenotypeSet.MeasureSet.measure.mrelationship.sequenceL.referenceVCFAllele", ttt.getTextContent());
                                                     iter.add(t2);
                                                              System.out.println("|    |    |    |    |    |    |-- referenceAlleleVCF: " + ttt.getTextContent());// + sssubnode.getTextContent());
                                                          }   
                                                          if (ttt.getNodeName().equals("alternateAlleleVCF")) {
                                                     Tuple2<String, String> t2 = new Tuple2<String, String>("ReferenceClinvarAssertion.GenotypeSet.MeasureSet.measure.mrelationship.sequenceL.alternateFAlleleVCF", ttt.getTextContent());
                                                     iter.add(t2);
                                                              System.out.println("|    |    |    |    |    |    |-- alternateAlleleVCF: " + ttt.getTextContent());// + sssubnode.getTextContent());
                                                          }   
                                                          if (ttt.getNodeName().equals("Strand")) {
                                                     Tuple2<String, String> t2 = new Tuple2<String, String>("ReferenceClinvarAssertion.GenotypeSet.MeasureSet.measure.mrelationship.sequenceL.Strand", ttt.getTextContent());
                                                     iter.add(t2);
                                                              System.out.println("|    |    |    |    |    |    |-- Strand: " + ttt.getTextContent());// + sssubnode.getTextContent());
                                                          }   
                                                      }   
                                                }   
                                                if (att.getNodeName().equals("XRef")) { 
                                                    System.out.println("|    |    |    |    |    |-- XRef ");//  + att.getTextContent());
                                                    NamedNodeMap mattsa = att.getAttributes();
                                                    for (int h = 0; h < mattsa.getLength(); h++) {
                                                       Node attl= mattsa.item(h);
                                                       if (attl.getNodeName().equals("ID")) {
                                                     Tuple2<String, String> t2 = new Tuple2<String, String>("ReferenceClinvarAssertion.GenotypeSet.MeasureSet.measure.XRef.ID", attl.getTextContent());
                                                     iter.add(t2);
                                                            System.out.println("|    |    |    |    |    |    |-- ID " + attl.getTextContent());//  + att.getTextContent());
                                                       }
                                                       if (attl.getNodeName().equals("DB")) {
                                                     Tuple2<String, String> t2 = new Tuple2<String, String>("ReferenceClinvarAssertion.GenotypeSet.MeasureSet.measure.XRef.BD", attl.getTextContent());
                                                     iter.add(t2);
                                                            System.out.println("|    |    |    |    |    |    |-- DB " + attl.getTextContent());//  + att.getTextContent());
                                                       }
                                                    }
                                                }

                                            }
                                      
                                          } // end of ref_geno_mset_attributeset

                                         

                                      
                                      }

                                      
                                   }
                                      
                                      
                                
                               }
                          }   

    
                     }   

                     Node ref_MeasureSet = e.getElementsByTagName("MeasureSet").item(0);
                      System.out.println("|    |-- MeasureSet");

                     NamedNodeMap refatt = ref_MeasureSet.getAttributes();
                      if(refatt.item(1).getNodeName().equals("ID")) {
                                                     Tuple2<String, String> t2 = new Tuple2<String, String>("ReferenceClinvarAssertion.MeasureSet.ID", refatt.item(1).getTextContent());
                                                     iter.add(t2);
                           System.out.println("|    |    |-- ID: " + refatt.item(1).getTextContent());
                     }
                     if(refatt.item(0).getNodeName().equals("Type")) {
                                                     Tuple2<String, String> t2 = new Tuple2<String, String>("ReferenceClinvarAssertion.MeasureSet.Type", refatt.item(0).getTextContent());
                                                     iter.add(t2);
                           System.out.println("|    |    |-- Type: " + refatt.item(0).getTextContent());
                     }

                     
                     NodeList refmlist = ref_MeasureSet.getChildNodes();
                       if (refmlist.getLength() > 1 ) {
                       ref_sum = ref_sum + 1000;
    //   System.out.println(" ref_sum " + ref_sum);
                       }
                      for (int i =0; i< refmlist.getLength(); i++) {

                         Node att = refmlist.item(i);

                         if (att.getNodeName().equals("Name")) {

                              System.out.println("|    |    |-- Name ");//  + att.getTextContent());
                              NodeList attlistn = att.getChildNodes();
                              for (int m=0; m<attlistn.getLength(); m++) {
                                   Node name = attlistn.item(m);
                                   if (name.getNodeName().equals("ElementValue")) {
                                                     Tuple2<String, String> t2 = new Tuple2<String, String>("ReferenceClinvarAssertion.MeasureSet.Name.ElementValue", name.getTextContent());
                                                     iter.add(t2);
                                        System.out.println("|    |    |    |    |-- ElementValue "  + name.getTextContent());
                                   }
                                   if (name.getNodeName().equals("XRef")) {
                                        System.out.println("|    |    |    |    |-- XRef ");//  + name.getTextContent());
                                        NamedNodeMap natt = name.getAttributes();
                                        for (int h = 0 ; h< natt.getLength(); h++) {
                                             if (natt.item(h).getNodeName().equals("ID")) {
                                                     Tuple2<String, String> t2 = new Tuple2<String, String>("ReferenceClinvarAssertion.MeasureSet.XRef.ID", natt.item(h).getTextContent());
                                                     iter.add(t2);
                                                 System.out.println("|    |    |    |    |    |-- ID " + natt.item(h).getTextContent());//  + name.getTextContent());
                                             }
                                             if (natt.item(h).getNodeName().equals("DB")) {
                                                     Tuple2<String, String> t2 = new Tuple2<String, String>("ReferenceClinvarAssertion.MeasureSet.XRef.DB", natt.item(h).getTextContent());
                                                     iter.add(t2);
                                                 System.out.println("|    |    |    |    |    |-- DB " + natt.item(h).getTextContent());//  + name.getTextContent());
                                             }
                                        }
                                    }
                              }

                         }

                         if (att.getNodeName().equals("AttributeSet")) {

                              System.out.println("|    |    |-- AttributeSet ");//  + att.getTextContent());
                              NodeList attlistn = att.getChildNodes();
                              for (int m=0; m<attlistn.getLength(); m++) {
                                   Node name = attlistn.item(m);
                                   if (name.getNodeName().equals("Attribute")) {
                                                     Tuple2<String, String> t2 = new Tuple2<String, String>("ReferenceClinvarAssertion.MeasureSet.AttributeSet.Attribute", name.getTextContent());
                                                     iter.add(t2);
                                        System.out.println("|    |    |    |    |-- Attribute "  + name.getTextContent());
                                   }   
                                   if (name.getNodeName().equals("XRef")) {
                                        System.out.println("|    |    |    |    |-- XRef ");//  + name.getTextContent());
                                        NamedNodeMap natt = name.getAttributes();
                                        for (int h = 0 ; h< natt.getLength(); h++) {
                                             if (natt.item(h).getNodeName().equals("ID")) {
                                                     Tuple2<String, String> t2 = new Tuple2<String, String>("ReferenceClinvarAssertion.MeasureSet.AttributeSet.XRef.ID", natt.item(h).getTextContent());
                                                     iter.add(t2);
                                                 System.out.println("|    |    |    |    |    |-- ID " + natt.item(h).getTextContent());//  + name.getTextContent());
                                             }   
                                             if (natt.item(h).getNodeName().equals("DB")) {
                                                     Tuple2<String, String> t2 = new Tuple2<String, String>("ReferenceClinvarAssertion.MeasureSet.AttributeSet.XRef.DB", natt.item(h).getTextContent());
                                                     iter.add(t2);
                                                 System.out.println("|    |    |    |    |    |-- DB " + natt.item(h).getTextContent());//  + name.getTextContent());
                                             }   
                                        }   
                                    }   

                              }   

                         }   

                         // MeasureSet_measure
                          if (att.getNodeName().equals("Measure")) {

                               ref_sum = ref_sum + 200;

                                      System.out.println("|    |    |    |-- Measure " );

                                      NamedNodeMap matts = att.getAttributes();
                                      for (int k = 0; k < matts.getLength(); k ++ ) {
                                         Node attms = matts.item(k);
                                         if (attms.getNodeName().equals("ID")) {
                                                     Tuple2<String, String> t2 = new Tuple2<String, String>("ReferenceClinvarAssertion.MeasureSet.Measure.ID", attms.getTextContent());
                                                     iter.add(t2);
                                             System.out.println("|    |    |    |    |-- ID " + attms.getTextContent());
                                         }
                                         if (attms.getNodeName().equals("Type")) {
                                                     Tuple2<String, String> t2 = new Tuple2<String, String>("ReferenceClinvarAssertion.MeasureSet.Measure.Type", attms.getTextContent());
                                                     iter.add(t2);
                                             System.out.println("|    |    |    |    |-- Type " + attms.getTextContent());
                                         }
                                      }

                                      NodeList ssubnodelist = att.getChildNodes();

                                      for (int k = 0; k < ssubnodelist.getLength(); k++) {
                                         Node sssubnode = ssubnodelist.item(k);

                                         // ref_genotypset_mset_measure_name
                                         if (sssubnode.getNodeName().equals("Name")) {

                                             System.out.println("|    |    |    |    |-- Name ");// + sssubnode.getTextContent());

                                             NodeList attlist = sssubnode.getChildNodes();
                                             for (int m=0; m<attlist.getLength(); m++) {
                                                Node attms = attlist.item(m);
                                                if (attms.getNodeName().equals("ElementValue")) {
                                                     Tuple2<String, String> t2 = new Tuple2<String, String>("ReferenceClinvarAssertion.MeasureSet.Measure.Name.ElementValue", attms.getTextContent());
                                                     iter.add(t2);
                                                    System.out.println("|    |    |    |    |    |-- ElementValue "  + attms.getTextContent());
                                                }

                                                if (attms.getNodeName().equals("XRef")) {
                                                     System.out.println("|    |    |    |    |    |-- XRef "  + attms.getTextContent());
                                                }
                                             }

                                         }
                                         // ref_mset_measure_CytogeneticLocation
                                         if (sssubnode.getNodeName().equals("CytogeneticLocation")) {
                                                     Tuple2<String, String> t2 = new Tuple2<String, String>("ReferenceClinvarAssertion.MeasureSet.Measure.CytogeneticLocation", sssubnode.getTextContent());
                                                     iter.add(t2);
                                            System.out.println("|    |    |    |    |-- CytogeneticLocation " + sssubnode.getTextContent());
                                         }

                                         // ref_mset_measure_comment
                                         if (sssubnode.getNodeName().equals("Comment")) {
                                                     Tuple2<String, String> t2 = new Tuple2<String, String>("ReferenceClinvarAssertion.MeasureSet.Measure.Comment", sssubnode.getTextContent());
                                                     iter.add(t2);
                                            System.out.println("|    |    |    |    |-- Comment " + sssubnode.getTextContent());
                                         }

                                         // ref_mset_measure_source
                                         if (sssubnode.getNodeName().equals("Source")) {
                                                     Tuple2<String, String> t2 = new Tuple2<String, String>("ReferenceClinvarAssertion.MeasureSet.Measure.Source", sssubnode.getTextContent());
                                                     iter.add(t2);
                                            System.out.println("|    |    |    |    |-- Source " + sssubnode.getTextContent());
                                         }

                                         // ref_mset_measure_sequenceLocation
                                         if (sssubnode.getNodeName().equals("SequenceLocation")) {
                                             System.out.println("|    |    |    |    |-- SequenceLocation ");// + sssubnode.getTextContent());
                                             NamedNodeMap sq = sssubnode.getAttributes();
                                             for (int m = 0; m < sq.getLength(); m++) {
                                                  Node attms = sq.item(m);
                                                  if (attms.getNodeName().equals("Assembly")) {
                                                     Tuple2<String, String> t2 = new Tuple2<String, String>("ReferenceClinvarAssertion.MeasureSet.Measure.sqL.Assembly", attms.getTextContent());
                                                     iter.add(t2);
                                                      System.out.println("|    |    |    |    |    |-- Assembly: " + attms.getTextContent());// + sssubnode.getTextContent());
                                                  }
                                                  if (attms.getNodeName().equals("AssemblyAccessionVersion")) {
                                                     Tuple2<String, String> t2 = new Tuple2<String, String>("ReferenceClinvarAssertion.MeasureSet.Measure.sqL.AssemblyAccessVersion", attms.getTextContent());
                                                     iter.add(t2);
                                                      System.out.println("|    |    |    |    |    |-- AssemblyAccessVersion: " + attms.getTextContent());// + sssubnode.getTextContent());
                                                  }
                                                  if (attms.getNodeName().equals("AssemblyStatus")) {
                                                     Tuple2<String, String> t2 = new Tuple2<String, String>("ReferenceClinvarAssertion.MeasureSet.Measure.sqL.AssemblyStatus", attms.getTextContent());
                                                     iter.add(t2);
                                                      System.out.println("|    |    |    |    |    |-- AssemblyStatus: " + attms.getTextContent());// + sssubnode.getTextContent());
                                                  }
                                                  if (attms.getNodeName().equals("Chr")) {
                                                     Tuple2<String, String> t2 = new Tuple2<String, String>("ReferenceClinvarAssertion.MeasureSet.Measure.sqL.Chr", attms.getTextContent());
                                                     iter.add(t2);
                                                      System.out.println("|    |    |    |    |    |-- Chr: " + attms.getTextContent());// + sssubnode.getTextContent());
                                                  }
                                                  if (attms.getNodeName().equals("Accession")) {
                                                     Tuple2<String, String> t2 = new Tuple2<String, String>("ReferenceClinvarAssertion.MeasureSet.Measure.sqL.Accession", attms.getTextContent());
                                                     iter.add(t2);
                                                      System.out.println("|    |    |    |    |    |-- Accession: " + attms.getTextContent());// + sssubnode.getTextContent());
                                                  }
                                                  if (attms.getNodeName().equals("start")) {
                                                     Tuple2<String, String> t2 = new Tuple2<String, String>("ReferenceClinvarAssertion.MeasureSet.Measure.sqL.start", attms.getTextContent());
                                                     iter.add(t2);
                                                      System.out.println("|    |    |    |    |    |-- start: " + attms.getTextContent());// + sssubnode.getTextContent());
                                                  }
                                                  if (attms.getNodeName().equals("stop")) {
                                                     Tuple2<String, String> t2 = new Tuple2<String, String>("ReferenceClinvarAssertion.MeasureSet.Measure.sqL.stop", attms.getTextContent());
                                                     iter.add(t2);
                                                      System.out.println("|    |    |    |    |    |-- stop: " + attms.getTextContent());// + sssubnode.getTextContent());
                                                  }
                                                  if (attms.getNodeName().equals("display_start")) {
                                                     Tuple2<String, String> t2 = new Tuple2<String, String>("ReferenceClinvarAssertion.MeasureSet.Measure.sqL.display_start", attms.getTextContent());
                                                     iter.add(t2);
                                                      System.out.println("|    |    |    |    |    |-- display_start: " + attms.getTextContent());// + sssubnode.getTextContent());
                                                  }
                                                  if (attms.getNodeName().equals("variantLength")) {
                                                     Tuple2<String, String> t2 = new Tuple2<String, String>("ReferenceClinvarAssertion.MeasureSet.Measure.sqL.variantLength", attms.getTextContent());
                                                     iter.add(t2);
                                                      System.out.println("|    |    |    |    |    |-- variantLength: " + attms.getTextContent());// + sssubnode.getTextContent());
                                                  }
                                                  if (attms.getNodeName().equals("referenceAllele")) {
                                                     Tuple2<String, String> t2 = new Tuple2<String, String>("ReferenceClinvarAssertion.MeasureSet.Measure.sqL.referenceAllele", attms.getTextContent());
                                                     iter.add(t2);
                                                      System.out.println("|    |    |    |    |    |-- referenceAllele: " + attms.getTextContent());// + sssubnode.getTextContent());
                                                  }
                                                  if (attms.getNodeName().equals("alternateAllele")) {
                                                     Tuple2<String, String> t2 = new Tuple2<String, String>("ReferenceClinvarAssertion.MeasureSet.Measure.sqL.alternateAllele", attms.getTextContent());
                                                     iter.add(t2);
                                                      System.out.println("|    |    |    |    |    |-- alternateAllele: " + attms.getTextContent());// + sssubnode.getTextContent());
                                                  }
                                                  if (attms.getNodeName().equals("positionVCF")) {
                                                     Tuple2<String, String> t2 = new Tuple2<String, String>("ReferenceClinvarAssertion.MeasureSet.Measure.sqL.positionVCF", attms.getTextContent());
                                                     iter.add(t2);
                                                      System.out.println("|    |    |    |    |    |-- positionVCF: " + attms.getTextContent());// + sssubnode.getTextContent());
                                                  }
                                                  if (attms.getNodeName().equals("referenceAlleleVCF")) {
                                                     Tuple2<String, String> t2 = new Tuple2<String, String>("ReferenceClinvarAssertion.MeasureSet.Measure.sqL.referenceAlleleVCF", attms.getTextContent());
                                                     iter.add(t2);
                                                      System.out.println("|    |    |    |    |    |-- referenceAlleleVCF: " + attms.getTextContent());// + sssubnode.getTextContent());
                                                  }
                                                  if (attms.getNodeName().equals("alternateAlleleVCF")) {
                                                     Tuple2<String, String> t2 = new Tuple2<String, String>("ReferenceClinvarAssertion.MeasureSet.Measure.sqL.alternateAlleleVCF", attms.getTextContent());
                                                     iter.add(t2);
                                                      System.out.println("|    |    |    |    |    |-- alternateAlleleVCF: " + attms.getTextContent());// + sssubnode.getTextContent());
                                                  }
                                                  if (attms.getNodeName().equals("Strand")) {
                                                     Tuple2<String, String> t2 = new Tuple2<String, String>("ReferenceClinvarAssertion.MeasureSet.Measure.sqL.Strand", attms.getTextContent());
                                                     iter.add(t2);
                                                      System.out.println("|    |    |    |    |    |-- Strand: " + attms.getTextContent());// + sssubnode.getTextContent());
                                                  }
                                              }

                                         }
                                         // ref_Mset_measure_attributeSet
                                         if (sssubnode.getNodeName().equals("AttributeSet")) {
                                            System.out.println("|    |    |    |    |-- AttributeSet ");//  + ssubnode.getTextContent());
                                            NodeList attlist = sssubnode.getChildNodes();
                                            for (int m=0; m<attlist.getLength(); m++) {
                                                Node attms = attlist.item(m);
                                                if (attms.getNodeName().equals("Attribute")) {
                                                     Tuple2<String, String> t2 = new Tuple2<String, String>("ReferenceClinvarAssertion.MeasureSet.Measure.AttributeSet.Attribute", attms.getTextContent());
                                                     iter.add(t2);
                                                    System.out.println("|    |    |    |    |    |-- Attribute "  + attms.getTextContent());
                                                }
                                                if (attms.getNodeName().equals("Comment")) {
                                                     Tuple2<String, String> t2 = new Tuple2<String, String>("ReferenceClinvarAssertion.MeasureSet.Measure.AttributeSet.comment", attms.getTextContent());
                                                     iter.add(t2);
                                                    System.out.println("|    |    |    |    |    |-- Comment "  + attms.getTextContent());
                                                }
                                                
                                               if (attms.getNodeName().equals("Citation")) {
                                                    System.out.println("|    |    |    |-- Citation ");//  + attms.getTextContent());
                                                    NodeList attlistn = att.getChildNodes();
                                                    for (int h=0; h<attlistn.getLength(); h++) {
                                                      Node name = attlistn.item(h);
                                                      if (name.getNodeName().equals("Abbrev")) {
                                                     Tuple2<String, String> t2 = new Tuple2<String, String>("ReferenceClinvarAssertion.MeasureSet.Measure.AttributeSet.Citation.Abbrev", name.getTextContent());
                                                     iter.add(t2);
                                                         System.out.println("|    |    |    |    |-- Abbrev "  + name.getTextContent());
                                                      }
                                                      if (name.getNodeName().equals("ID")) {
                                                     Tuple2<String, String> t2 = new Tuple2<String, String>("ReferenceClinvarAssertion.MeasureSet.Measure.AttributeSet.Citation.ID", name.getTextContent());
                                                     iter.add(t2);
                                                         System.out.println("|    |    |    |    |-- ID "  + name.getTextContent());
                                                      }
                                                      if (name.getNodeName().equals("CitationText")) {
                                                     Tuple2<String, String> t2 = new Tuple2<String, String>("ReferenceClinvarAssertion.MeasureSet.Measure.AttributeSet.Citation.CitationText", name.getTextContent());
                                                     iter.add(t2);
                                                          System.out.println("|    |    |    |    |-- CitationText "  + name.getTextContent());
                                                      }
                                                      if (name.getNodeName().equals("URL")) {
                                                     Tuple2<String, String> t2 = new Tuple2<String, String>("ReferenceClinvarAssertion.MeasureSet.Measure.AttributeSet.Citation.URL", name.getTextContent());
                                                     iter.add(t2);
                                                          System.out.println("|    |    |    |    |-- URL "  + name.getTextContent());
                                                      }

                                                   }

                                                }

                                                if (att.getNodeName().equals("XRef")) {
                                                    System.out.println("|    |    |    |    |    |-- XRef ");//  + att.getTextContent());
                                                    NamedNodeMap mattsa = att.getAttributes();
                                                    for (int h = 0; h < mattsa.getLength(); h++) {
                                                       Node attl= mattsa.item(h);
                                                       if (attl.getNodeName().equals("ID")) {
                                                     Tuple2<String, String> t2 = new Tuple2<String, String>("ReferenceClinvarAssertion.MeasureSet.Measure.AttributeSet.XRef.ID", attl.getTextContent());
                                                     iter.add(t2);
                                                            System.out.println("|    |    |    |    |    |    |-- ID " + attl.getTextContent());//  + att.getTextContent());
                                                       }
                                                       if (attl.getNodeName().equals("DB")) {
                                                     Tuple2<String, String> t2 = new Tuple2<String, String>("ReferenceClinvarAssertion.MeasureSet.Measure.AttributeSet.XRef.DB", attl.getTextContent());
                                                     iter.add(t2);
                                                            System.out.println("|    |    |    |    |    |    |-- DB " + attl.getTextContent());//  + att.getTextContent());
                                                       }
                                                    }
                                                }
                                            }

                                          } // end of ref__mset_measure_attributeset
                                          // ref_genotypset_mset_measure_measurerelationship
                                         if (sssubnode.getNodeName().equals("MeasureRelationship")) {
                                             ref_sum = ref_sum + 200;

                                            System.out.println("|    |    |    |    |-- MeasureRelationship ");//  + ssubnode.getTextContent());
                                            NamedNodeMap mratt = sssubnode.getAttributes();
                                            if (mratt.item(0).getNodeName().equals("Type")) {
                                                     Tuple2<String, String> t2 = new Tuple2<String, String>("ReferenceClinvarAssertion.MeasureSet.Measure.Measurerelationship.Type", mratt.item(0).getTextContent());
                                                     iter.add(t2);
                                                System.out.println("|    |    |    |    |    |-- Type: " + mratt.item(0).getTextContent() );//  + ssubnode.getTextContent());
                                            }


                                            NodeList attlist = sssubnode.getChildNodes();
                                            for (int m=0; m<attlist.getLength(); m++) {
                                                Node attms = attlist.item(m);

                                                 if (attms.getNodeName().equals("Name")) {
                                                     System.out.println("|    |    |    |    |    |-- Name ");//  + attms.getTextContent());

                                                     NodeList attlistname = attms.getChildNodes();
                                                     for (int h=0; h<attlistname.getLength(); h++) {
                                                        Node attname = attlistname.item(h);
                                                        if (attname.getNodeName().equals("ElementValue")) {
                                                     Tuple2<String, String> t2 = new Tuple2<String, String>("ReferenceClinvarAssertion.MeasureSet.Measure.Measurerelationship.Name.ElementValue", attname.getTextContent());
                                                     iter.add(t2);
                                                            System.out.println("|    |    |    |    |    |    |-- ElementValue "  + attname.getTextContent());
                                                         }

                                                     }
                                                 }
                                                 if (attms.getNodeName().equals("Symbol")) {
                                                     System.out.println("|    |    |    |    |    |-- Symbol ");//  + attms.getTextContent());

                                                     NodeList attlistname = attms.getChildNodes();
                                                     for (int h=0; h<attlistname.getLength(); h++) {
                                                        Node attname = attlistname.item(h);
                                                        if (attname.getNodeName().equals("ElementValue")) {
                                                     Tuple2<String, String> t2 = new Tuple2<String, String>("ReferenceClinvarAssertion.MeasureSet.Measure.Measurerelationship.Symbol.ElementValue", attname.getTextContent());
                                                     iter.add(t2);
                                                            System.out.println("|    |    |    |    |    |    |-- ElementValue "  + attname.getTextContent());
                                                         }

                                                     }
                                                 }
                                                 if (attms.getNodeName().equals("Comment")) {
                                                     Tuple2<String, String> t2 = new Tuple2<String, String>("ReferenceClinvarAssertion.MeasureSet.Measure.Measurerelationship.Comment", attms.getTextContent());
                                                     iter.add(t2);
                                                     System.out.println("|    |    |    |    |    |-- Comment ");//  + attms.getTextContent());
                                                 }

                                                 if (attms.getNodeName().equals("SequenceLocation")) {
                                                      System.out.println("|    |    |    |    |    |-- SequenceLocation ");// + sssubnode.getTextContent());
                                                      NamedNodeMap sq = att.getAttributes();
                                                      for (int h = 0; h < sq.getLength(); h++) {
                                                          Node ttt = sq.item(h);
                                                          if (ttt.getNodeName().equals("Assembly")) {
                                                     Tuple2<String, String> t2 = new Tuple2<String, String>("ReferenceClinvarAssertion.MeasureSet.Measure.Measurerelationship.sql.Assembly", ttt.getTextContent());
                                                     iter.add(t2);
                                                            System.out.println("|    |    |    |    |    |    |-- Assembly: " + ttt.getTextContent());// + sssubnode.getTextContent());
                                                          }
                                                          if (ttt.getNodeName().equals("AssemblyAccessionVersion")) {
                                                     Tuple2<String, String> t2 = new Tuple2<String, String>("ReferenceClinvarAssertion.MeasureSet.Measure.Measurerelationship.sql.AssemblyAccessVersion", ttt.getTextContent());
                                                     iter.add(t2);
                                                             System.out.println("|    |    |    |    |    |    |-- AssemblyAccessVersion: " + ttt.getTextContent());// + sssubnode.getTextContent());
                                                          }
                                                          if (ttt.getNodeName().equals("AssemblyStatus")) {
                                                     Tuple2<String, String> t2 = new Tuple2<String, String>("ReferenceClinvarAssertion.MeasureSet.Measure.Measurerelationship.sql.AssemblyStatus", ttt.getTextContent());
                                                     iter.add(t2);
                                                             System.out.println("|    |    |    |    |    |    |-- AssemblyStatus: " + ttt.getTextContent());// + sssubnode.getTextContent());
                                                          }
                                                          if (ttt.getNodeName().equals("Chr")) {
                                                     Tuple2<String, String> t2 = new Tuple2<String, String>("ReferenceClinvarAssertion.MeasureSet.Measure.Measurerelationship.sql.Chr", ttt.getTextContent());
                                                     iter.add(t2);
                                                             System.out.println("|    |    |    |    |    |    |-- Chr: " + ttt.getTextContent());// + sssubnode.getTextContent());
                                                          }
                                                          if (ttt.getNodeName().equals("Accession")) {
                                                     Tuple2<String, String> t2 = new Tuple2<String, String>("ReferenceClinvarAssertion.MeasureSet.Measure.Measurerelationship.sql.Accession", ttt.getTextContent());
                                                     iter.add(t2);
                                                             System.out.println("|    |    |    |    |    |    |-- Accession: " + ttt.getTextContent());// + sssubnode.getTextContent());
                                                          }
                                                          if (ttt.getNodeName().equals("start")) {
                                                     Tuple2<String, String> t2 = new Tuple2<String, String>("ReferenceClinvarAssertion.MeasureSet.Measure.Measurerelationship.sql.start", ttt.getTextContent());
                                                     iter.add(t2);
                                                             System.out.println("|    |    |    |    |    |    |-- Accession: " + ttt.getTextContent());// + sssubnode.getTextContent());
                                                              System.out.println("|    |    |    |    |    |    |-- start: " + ttt.getTextContent());// + sssubnode.getTextContent());
                                                          }
                                                          if (ttt.getNodeName().equals("stop")) {
                                                     Tuple2<String, String> t2 = new Tuple2<String, String>("ReferenceClinvarAssertion.MeasureSet.Measure.Measurerelationship.sql.stop", ttt.getTextContent());
                                                     iter.add(t2);
                                                             System.out.println("|    |    |    |    |    |    |-- Accession: " + ttt.getTextContent());// + sssubnode.getTextContent());
                                                              System.out.println("|    |    |    |    |    |    |-- stop: " + ttt.getTextContent());// + sssubnode.getTextContent());
                                                          }
                                                          if (ttt.getNodeName().equals("display_start")) {
                                                     Tuple2<String, String> t2 = new Tuple2<String, String>("ReferenceClinvarAssertion.MeasureSet.Measure.Measurerelationship.sql.display_start", ttt.getTextContent());
                                                     iter.add(t2);
                                                             System.out.println("|    |    |    |    |    |    |-- Accession: " + ttt.getTextContent());// + sssubnode.getTextContent());
                                                              System.out.println("|    |    |    |    |    |    |-- display_start: " + ttt.getTextContent());// + sssubnode.getTextContent());
                                                          }
                                                          if (ttt.getNodeName().equals("variantLength")) {
                                                     Tuple2<String, String> t2 = new Tuple2<String, String>("ReferenceClinvarAssertion.MeasureSet.Measure.Measurerelationship.sql.variantLength", ttt.getTextContent());
                                                     iter.add(t2);
                                                              System.out.println("|    |    |    |    |    |    |-- variantLength: " + ttt.getTextContent());// + sssubnode.getTextContent());
                                                          }
                                                          if (ttt.getNodeName().equals("referenceAllele")) {
                                                     Tuple2<String, String> t2 = new Tuple2<String, String>("ReferenceClinvarAssertion.MeasureSet.Measure.Measurerelationship.sql.referenceAllele", ttt.getTextContent());
                                                     iter.add(t2);
                                                              System.out.println("|    |    |    |    |    |    |-- referenceAllele: " + ttt.getTextContent());// + sssubnode.getTextContent());
                                                          }
                                                          if (ttt.getNodeName().equals("alternateAllele")) {
                                                     Tuple2<String, String> t2 = new Tuple2<String, String>("ReferenceClinvarAssertion.MeasureSet.Measure.Measurerelationship.sql.alternateAllele", ttt.getTextContent());
                                                     iter.add(t2);
                                                              System.out.println("|    |    |    |    |    |    |-- alternateAllele: " + ttt.getTextContent());// + sssubnode.getTextContent());
                                                          }
                                                          if (ttt.getNodeName().equals("positionVCF")) {
                                                     Tuple2<String, String> t2 = new Tuple2<String, String>("ReferenceClinvarAssertion.MeasureSet.Measure.Measurerelationship.sql.positionVCF", ttt.getTextContent());
                                                     iter.add(t2);
                                                              System.out.println("|    |    |    |    |    |    |-- positionVCF: " + ttt.getTextContent());// + sssubnode.getTextContent());
                                                          }
                                                          if (ttt.getNodeName().equals("referenceAlleVCF")) {
                                                     Tuple2<String, String> t2 = new Tuple2<String, String>("ReferenceClinvarAssertion.MeasureSet.Measure.Measurerelationship.sql.referenceAlleleVCF", ttt.getTextContent());
                                                     iter.add(t2);
                                                              System.out.println("|    |    |    |    |    |    |-- referenceAlleleVCF: " + ttt.getTextContent());// + sssubnode.getTextContent());
                                                          }
                                                          if (ttt.getNodeName().equals("alternateAlleleVCF")) {
                                                     Tuple2<String, String> t2 = new Tuple2<String, String>("ReferenceClinvarAssertion.MeasureSet.Measure.Measurerelationship.sql.alternateAlleleVCF", ttt.getTextContent());
                                                     iter.add(t2);
                                                              System.out.println("|    |    |    |    |    |    |-- alternateAlleleVCF: " + ttt.getTextContent());// + sssubnode.getTextContent());
                                                          }
                                                          if (ttt.getNodeName().equals("Strand")) {
                                                     Tuple2<String, String> t2 = new Tuple2<String, String>("ReferenceClinvarAssertion.MeasureSet.Measure.Measurerelationship.sql.Strand", ttt.getTextContent());
                                                     iter.add(t2);
                                                              System.out.println("|    |    |    |    |    |    |-- Strand: " + ttt.getTextContent());// + sssubnode.getTextContent());
                                                          }
                                                      }
                                                }


                                                if (att.getNodeName().equals("XRef")) {
                                                    System.out.println("|    |    |    |    |    |-- XRef ");//  + att.getTextContent());
                                                    NamedNodeMap mattsa = att.getAttributes();
                                                    for (int h = 0; h < mattsa.getLength(); h++) {
                                                       Node attl= mattsa.item(h);
                                                       if (attl.getNodeName().equals("ID")) {
                                                     Tuple2<String, String> t2 = new Tuple2<String, String>("ReferenceClinvarAssertion.MeasureSet.Measure.Measurerelationship.XRef.ID", attl.getTextContent());
                                                     iter.add(t2);
                                                            System.out.println("|    |    |    |    |    |    |-- ID " + attl.getTextContent());//  + att.getTextContent());
                                                       }
                                                       if (attl.getNodeName().equals("DB")) {
                                                     Tuple2<String, String> t2 = new Tuple2<String, String>("ReferenceClinvarAssertion.MeasureSet.Measure.Measurerelationship.XRef.DB", attl.getTextContent());
                                                     iter.add(t2);
                                                            System.out.println("|    |    |    |    |    |    |-- DB " + attl.getTextContent());//  + att.getTextContent());
                                                       }
                                                    }
                                                }

                                            }

                                          } // end of ref_geno_mset_attributeset




                        
                         }
                        }

 

                         if (att.getNodeName().equals("Symbol")) {
                                               System.out.println("|    |    |-- Symbol ");//  + att.getTextContent());
                                               NodeList attlists = att.getChildNodes();
                                             for (int m=0; m<attlists.getLength(); m++) {
                                                Node name = attlists.item(m);
                                                if (name.getNodeName().equals("ElementValue")) {
                                                     Tuple2<String, String> t2 = new Tuple2<String, String>("ReferenceClinvarAssertion.MeasureSet.Symbol.ElementValue", name.getTextContent());
                                                     iter.add(t2);
                                                    System.out.println("|    |    |    |-- ElementValue "  + name.getTextContent());
                                                }
                                             }

                         }
                         if (att.getNodeName().equals("XRef")) {
                              System.out.println("|    |    |-- XRef ");//  + att.getTextContent());
                              NamedNodeMap mattsa = att.getAttributes();
                              for (int h = 0; h < mattsa.getLength(); h++) {
                                   Node attl= mattsa.item(h);
                                   if (attl.getNodeName().equals("ID")) {
                                                     Tuple2<String, String> t2 = new Tuple2<String, String>("ReferenceClinvarAssertion.MeasureSet.XRef.ID", attl.getTextContent());
                                                     iter.add(t2);
                                      System.out.println("|    |    |    |-- ID " + attl.getTextContent());//  + att.getTextContent());
                                   }
                                   if (attl.getNodeName().equals("DB")) {
                                                     Tuple2<String, String> t2 = new Tuple2<String, String>("ReferenceClinvarAssertion.MeasureSet.XRef.DB", attl.getTextContent());
                                                     iter.add(t2);
                                      System.out.println("|    |    |    |-- DB " + attl.getTextContent());//  + att.getTextContent());
                                   }
                               }
                          }


                      }
                      
                      // process ref_observedINset
                      Node ref_ObservedInSet = e.getElementsByTagName("ObservedIn").item(0);
            
                      System.out.println("|    |-- ObervedIn");
                      NodeList ref_oblist = ref_ObservedInSet.getChildNodes();
                       if (ref_oblist.getLength() > 1 ) {
                       ref_sum = ref_sum + 1000;
   //    System.out.println(" ref_sum " + ref_sum);
                       }
                      for (int i =0 ; i< ref_oblist.getLength(); i++) {
                          Node subnode = ref_oblist.item(i);
                           if (subnode.getNodeName().equals("Method")) {
                               System.out.println("|    |    |-- Method: ");// + subnode.getTextContent());
                               NodeList attlist = subnode.getChildNodes();
                               for (int k=0; k<attlist.getLength(); k++) {
                                      Node att = attlist.item(k);
                                      if (att.getNodeName().equals("MethodType")) {
                                                     Tuple2<String, String> t2 = new Tuple2<String, String>("ReferenceClinvarAssertion.ObservedInSet.Method.MethodType", att.getTextContent());
                                                     iter.add(t2);
                                               System.out.println("|    |    |    |-- MethodType "  + att.getTextContent());
                                       }
                                      if (att.getNodeName().equals("NamePlatform")) {
                                                     Tuple2<String, String> t2 = new Tuple2<String, String>("ReferenceClinvarAssertion.ObservedInSet.Method.NamePlatform", att.getTextContent());
                                                     iter.add(t2);
                                               System.out.println("|    |    |    |-- NamePlatform "  + att.getTextContent());
                                       }
                                      if (att.getNodeName().equals("Purpose")) {
                                                     Tuple2<String, String> t2 = new Tuple2<String, String>("ReferenceClinvarAssertion.ObservedInSet.Method.Purpose", att.getTextContent());
                                                     iter.add(t2);
                                               System.out.println("|    |    |    |-- Purpose "  + att.getTextContent());
                                       }
                                      if (att.getNodeName().equals("ResultType")) {
                                               System.out.println("|    |    |    |-- ResultType "  + att.getTextContent());
                                                     Tuple2<String, String> t2 = new Tuple2<String, String>("ReferenceClinvarAssertion.ObservedInSet.Method.ResultType", att.getTextContent());
                                                     iter.add(t2);
                                       }
                                      if (att.getNodeName().equals("Description")) {
                                                     Tuple2<String, String> t2 = new Tuple2<String, String>("ReferenceClinvarAssertion.ObservedInSet.Method.Description", att.getTextContent());
                                                     iter.add(t2);
                                               System.out.println("|    |    |    |-- Description "  + att.getTextContent());
                                       }
                                }

                          }
                           if (subnode.getNodeName().equals("Sample")) {
                               System.out.println("|    |    |-- Sample: ");// + subnode.getTextContent());
                               NodeList attlist = subnode.getChildNodes();
                               for (int k=0; k<attlist.getLength(); k++) {
                                      Node att = attlist.item(k);
                                      if (att.getNodeName().equals("AffectedStatus")) {
                                                     Tuple2<String, String> t2 = new Tuple2<String, String>("ReferenceClinvarAssertion.ObservedInSet.Sample.AffectedStatus", att.getTextContent());
                                                     iter.add(t2);
                                               System.out.println("|    |    |    |-- AffectedStatus "  + att.getTextContent());
                                       }
                                      if (att.getNodeName().equals("Ethnicity")) {
                                                     Tuple2<String, String> t2 = new Tuple2<String, String>("ReferenceClinvarAssertion.ObservedInSet.Sample.Ethnicity", att.getTextContent());
                                                     iter.add(t2);
                                               System.out.println("|    |    |    |-- Ethnicity "  + att.getTextContent());
                                       }
                                      if (att.getNodeName().equals("FamilyData")) {
                                                     Tuple2<String, String> t2 = new Tuple2<String, String>("ReferenceClinvarAssertion.ObservedInSet.Sample.FamilyData", att.getTextContent());
                                                     iter.add(t2);
                                               System.out.println("|    |    |    |-- FamilyData "  + att.getTextContent());
                                       }
                                      if (att.getNodeName().equals("NumberFemales")) {
                                                     Tuple2<String, String> t2 = new Tuple2<String, String>("ReferenceClinvarAssertion.ObservedInSet.Sample.NumberFemales", att.getTextContent());
                                                     iter.add(t2);
                                               System.out.println("|    |    |    |-- NumberFemales "  + att.getTextContent());
                                       }
                                      if (att.getNodeName().equals("NumberMales")) {
                                                     Tuple2<String, String> t2 = new Tuple2<String, String>("ReferenceClinvarAssertion.ObservedInSet.Sample.NumberMales", att.getTextContent());
                                                     iter.add(t2);
                                               System.out.println("|    |    |    |-- NumberMales "  + att.getTextContent());
                                       }
                                      if (att.getNodeName().equals("NumberTested")) {
                                                     Tuple2<String, String> t2 = new Tuple2<String, String>("ReferenceClinvarAssertion.ObservedInSet.Sample.NumberTested", att.getTextContent());
                                                     iter.add(t2);
                                               System.out.println("|    |    |    |-- NumberTested "  + att.getTextContent());
                                       }
                                      if (att.getNodeName().equals("Origin")) {
                                                     Tuple2<String, String> t2 = new Tuple2<String, String>("ReferenceClinvarAssertion.ObservedInSet.Sample.Origin", att.getTextContent());
                                                     iter.add(t2);
                                               System.out.println("|    |    |    |-- Origin "  + att.getTextContent());
                                       }
                                      if (att.getNodeName().equals("Species")) {
                                                     Tuple2<String, String> t2 = new Tuple2<String, String>("ReferenceClinvarAssertion.ObservedInSet.Sample.Species", att.getTextContent());
                                                     iter.add(t2);
                                               System.out.println("|    |    |    |-- Species "  + att.getTextContent());
                                      }
                               }
                           }
                        
                           // ObservedIn_ObservedData 
                           if (subnode.getNodeName().equals("ObservedData")) {
                               System.out.println("|    |    |-- ObservedData: ");// + subnode.getTextContent());
                               NamedNodeMap tstraitatts = subnode.getAttributes();
                               if(tstraitatts.item(0).getNodeName().equals("ID")) {
                                                     Tuple2<String, String> t2 = new Tuple2<String, String>("ReferenceClinvarAssertion.ObservedInSet.ObservedData.ID", tstraitatts.item(0).getTextContent());
                                                     iter.add(t2);
                                      System.out.println("|    |    |    |-- ID: " + tstraitatts.item(0).getTextContent());
                               }
                               if(tstraitatts.item(1).getNodeName().equals("Type")) {
                                                     Tuple2<String, String> t2 = new Tuple2<String, String>("ReferenceClinvarAssertion.ObservedInSet.ObservedData.Type", tstraitatts.item(1).getTextContent());
                                                     iter.add(t2);
                                   System.out.println("|    |    |-- Type: " + tstraitatts.item(1).getTextContent());
                               }


                               NodeList attlist = subnode.getChildNodes();

                               for (int j=0; j<attlist.getLength(); j++) {
                                      Node att = attlist.item(j);

                                      if (att.getNodeName().equals("Attribute")) {
                                                     Tuple2<String, String> t2 = new Tuple2<String, String>("ReferenceClinvarAssertion.ObservedInSet.ObservedData.Attribute", att.getTextContent());
                                                     iter.add(t2);
                                          System.out.println("|    |    |    |-- Attribute: " + att.getTextContent());
                                      }

                                      if (att.getNodeName().equals("Citation")) {
                                               System.out.println("|    |    |    |-- Citation ");//  + att.getTextContent());
                                               NodeList attlistn = att.getChildNodes();
                                             for (int m=0; m<attlistn.getLength(); m++) {
                                                Node name = attlistn.item(m);
                                                if (name.getNodeName().equals("Abbrev")) {
                                                     Tuple2<String, String> t2 = new Tuple2<String, String>("ReferenceClinvarAssertion.ObservedInSet.ObservedData.Citation.Abbrev", name.getTextContent());
                                                     iter.add(t2);
                                                    System.out.println("|    |    |    |    |-- Abbrev "  + name.getTextContent());
                                                }
                                                if (name.getNodeName().equals("ID")) {
                                                     Tuple2<String, String> t2 = new Tuple2<String, String>("ReferenceClinvarAssertion.ObservedInSet.ObservedData.Citation.ID", name.getTextContent());
                                                     iter.add(t2);
                                                    System.out.println("|    |    |    |    |-- ID "  + name.getTextContent());
                                                }
                                                if (name.getNodeName().equals("CitationText")) {
                                                     Tuple2<String, String> t2 = new Tuple2<String, String>("ReferenceClinvarAssertion.ObservedInSet.ObservedData.Citation.CitationText", name.getTextContent());
                                                     iter.add(t2);
                                                    System.out.println("|    |    |    |    |-- CitationText "  + name.getTextContent());
                                                }
                                                if (name.getNodeName().equals("URL")) {
                                                     Tuple2<String, String> t2 = new Tuple2<String, String>("ReferenceClinvarAssertion.ObservedInSet.ObservedData.Citation.URL", name.getTextContent());
                                                     iter.add(t2);
                                                    System.out.println("|    |    |    |    |-- URL "  + name.getTextContent());
                                                }

                                             }

                                      }
                              }
                      } 
                    }

                     // process ref_traitSet                  
              
                     Node ref_TraitSet = e.getElementsByTagName("TraitSet").item(0);


                     System.out.println("|    |-- TraitSet:");
                     NamedNodeMap tsatts = ref_TraitSet.getAttributes();
                     if(tsatts.item(1).getNodeName().equals("ID")) {
                                                     Tuple2<String, String> t2 = new Tuple2<String, String>("ReferenceClinvarAssertion.TraitSet.ID", tsatts.item(1).getTextContent());
                                                     iter.add(t2);
                           System.out.println("|    |    |-- ID: " + tsatts.item(1).getTextContent());
                     }
                     if(tsatts.item(0).getNodeName().equals("Type")) {
                                                     Tuple2<String, String> t2 = new Tuple2<String, String>("ReferenceClinvarAssertion.TraitSet.Type", tsatts.item(0).getTextContent());
                                                     iter.add(t2);
                           System.out.println("|    |    |-- Type: " + tsatts.item(0).getTextContent());
                     }


                     NodeList tslist = ref_TraitSet.getChildNodes();
                       if (tslist.getLength() > 1 ) {
                       ref_sum = ref_sum + 1000;
       System.out.println(" ref_sum " + ref_sum);
                       }


                     for (int i =0 ; i< tslist.getLength(); i++) {
                          Node subnode = tslist.item(i);
                           if (subnode.getNodeName().equals("Name")) {
                               System.out.println("|    |    |-- Name: ");// + subnode.getTextContent());
                               NodeList attlist = subnode.getChildNodes();
                               for (int k=0; k<attlist.getLength(); k++) {
                                      Node att = attlist.item(k);
                                      if (att.getNodeName().equals("ElementValue")) {
                                                     Tuple2<String, String> t2 = new Tuple2<String, String>("ReferenceClinvarAssertion.TraitSet.Name.ElementValue", att.getTextContent());
                                                     iter.add(t2);
                                               System.out.println("|    |    |    | -- ElementValue "  + att.getTextContent());
                                       }
                                }

                          }
                           // traitset_trait
                           if (subnode.getNodeName().equals("Trait")) {
                               System.out.println("|    |    |-- Trait: ");// + subnode.getTextContent());
                               NamedNodeMap tstraitatts = subnode.getAttributes();
                               if(tstraitatts.item(0).getNodeName().equals("ID")) {
                                                     Tuple2<String, String> t2 = new Tuple2<String, String>("ReferenceClinvarAssertion.TraitSet.Trait.ID", tstraitatts.item(0).getTextContent());
                                                     iter.add(t2);
                                      System.out.println("|    |    |    |-- ID: " + tstraitatts.item(0).getTextContent());
                               }
                               if(tsatts.item(1).getNodeName().equals("Type")) {
                                                     Tuple2<String, String> t2 = new Tuple2<String, String>("ReferenceClinvarAssertion.TraitSet.Trait.Type", tstraitatts.item(1).getTextContent());
                                                     iter.add(t2);
                                   System.out.println("|    |    |-- Type: " + tstraitatts.item(1).getTextContent());
                               }


                               NodeList attlist = subnode.getChildNodes();

                               for (int j=0; j<attlist.getLength(); j++) {
                                      Node att = attlist.item(j);

                                      if (att.getNodeName().equals("Name")) {
                                               System.out.println("|    |    |    |-- Name ");//  + att.getTextContent());
                                               NodeList attlistn = att.getChildNodes();
                                             for (int m=0; m<attlistn.getLength(); m++) {
                                                Node name = attlistn.item(m);
                                                if (name.getNodeName().equals("ElementValue")) {
                                                     Tuple2<String, String> t2 = new Tuple2<String, String>("ReferenceClinvarAssertion.TraitSet.Trait.Name.ElementValue", name.getTextContent());
                                                     iter.add(t2);
                                                    System.out.println("|    |    |    |    |-- ElementValue "  + name.getTextContent());
                                                }
                                                if (name.getNodeName().equals("XRef")) {
                                                     System.out.println("|    |    |    |    |-- XRef ");//  + name.getTextContent());
                                                     NamedNodeMap natt = name.getAttributes();
                                                     for (int h = 0 ; h< natt.getLength(); h++) {
                                                       if (natt.item(h).getNodeName().equals("ID")) {
                                                     Tuple2<String, String> t2 = new Tuple2<String, String>("ReferenceClinvarAssertion.TraitSet.Trait.Name.XRef.ID", natt.item(h).getTextContent());
                                                     iter.add(t2);
                                                           System.out.println("|    |    |    |    |    |-- ID " + natt.item(h).getTextContent());//  + name.getTextContent());
                                                       }
                                                       if (natt.item(h).getNodeName().equals("DB")) {
                                                     Tuple2<String, String> t2 = new Tuple2<String, String>("ReferenceClinvarAssertion.TraitSet.Trait.Name.XRef.DB", natt.item(h).getTextContent());
                                                     iter.add(t2);
                                                           System.out.println("|    |    |    |    |    |-- DB " + natt.item(h).getTextContent());//  + name.getTextContent());
                                                       }
                                                     }
                                                }
                                             }
                                         
                                      }
                                      if (att.getNodeName().equals("Symbol")) {
                                               System.out.println("|    |    |    |-- Symbol ");//  + att.getTextContent());
                                               NodeList attlists = att.getChildNodes();
                                             for (int m=0; m<attlists.getLength(); m++) {
                                                Node name = attlists.item(m);
                                                if (name.getNodeName().equals("ElementValue")) {
                                                     Tuple2<String, String> t2 = new Tuple2<String, String>("ReferenceClinvarAssertion.TraitSet.Trait.Symbol.ElementValue", name.getTextContent());
                                                     iter.add(t2);
                                                    System.out.println("|    |    |    |    |-- ElementValue "  + name.getTextContent());
                                                }
                                                if (name.getNodeName().equals("XRef")) {
                                                     System.out.println("|    |    |    |    |-- XRef ");//  + name.getTextContent());
                                                     NamedNodeMap natt = name.getAttributes();
                                                     for (int h = 0 ; h< natt.getLength(); h++) {
                                                       if (natt.item(h).getNodeName().equals("ID")) {
                                                     Tuple2<String, String> t2 = new Tuple2<String, String>("ReferenceClinvarAssertion.TraitSet.Trait.Symbol.XRef.ID", natt.item(h).getTextContent());
                                                     iter.add(t2);
                                                           System.out.println("|    |    |    |    |    |-- ID " + natt.item(h).getTextContent());//  + name.getTextContent());
                                                       }
                                                       if (natt.item(h).getNodeName().equals("DB")) {
                                                     Tuple2<String, String> t2 = new Tuple2<String, String>("ReferenceClinvarAssertion.TraitSet.Trait.Symbol.XRef.DB", natt.item(h).getTextContent());
                                                     iter.add(t2);
                                                           System.out.println("|    |    |    |    |    |-- DB " + natt.item(h).getTextContent());//  + name.getTextContent());
                                                       }
                                                     }
                                                }
                                             }
                                      }

                                      if (att.getNodeName().equals("AttributeSet")) {
                                               System.out.println("|    |    |    |-- AttributeSet ");//  + att.getTextContent());
                                               NodeList attlista = att.getChildNodes();
                                             for (int m=0; m<attlista.getLength(); m++) {
                                                Node name = attlista.item(m);
                                                if (name.getNodeName().equals("Attribute")) {
                                                     Tuple2<String, String> t2 = new Tuple2<String, String>("ReferenceClinvarAssertion.TraitSet.Trait.AttributeSet.Attribute", name.getTextContent());
                                                     iter.add(t2);
                                                    System.out.println("|    |    |    |    |-- Attribute "  + name.getTextContent());
                                                }
                                                if (name.getNodeName().equals("XRef")) {
                                                     System.out.println("|    |    |    |    |-- XRef ");//  + name.getTextContent());
                                                     NamedNodeMap natt = name.getAttributes();
                                                     for (int h = 0 ; h< natt.getLength(); h++) {
                                                       if (natt.item(h).getNodeName().equals("ID")) {
                                                     Tuple2<String, String> t2 = new Tuple2<String, String>("ReferenceClinvarAssertion.TraitSet.Trait.AttributeSet.XRef.ID", natt.item(h).getTextContent());
                                                     iter.add(t2);
                                                           System.out.println("|    |    |    |    |    |-- ID " + natt.item(h).getTextContent());//  + name.getTextContent());
                                                       }
                                                       if (natt.item(h).getNodeName().equals("DB")) {
                                                     Tuple2<String, String> t2 = new Tuple2<String, String>("ReferenceClinvarAssertion.TraitSet.Trait.AttributeSet.XRef.DB", natt.item(h).getTextContent());
                                                     iter.add(t2);
                                                           System.out.println("|    |    |    |    |    |-- DB " + natt.item(h).getTextContent());//  + name.getTextContent());
                                                       }
                                                     }
                                                }
                                             }
                                      }


                                      if (att.getNodeName().equals("XRef")) {
                                                    System.out.println("|    |    |    |-- XRef ");//  + att.getTextContent());
                                                    NamedNodeMap mattsa = att.getAttributes();
                                                    for (int h = 0; h < mattsa.getLength(); h++) {
                                                       Node attl= mattsa.item(h);
                                                       if (attl.getNodeName().equals("ID")) {  
                                                     Tuple2<String, String> t2 = new Tuple2<String, String>("ReferenceClinvarAssertion.TraitSet.Trait.XRef.ID", attl.getTextContent());
                                                     iter.add(t2);
                                                            System.out.println("|    |    |    |    |-- ID " + attl.getTextContent());//  + att.getTextContent());
                                                       }
                                                       if (attl.getNodeName().equals("DB")) {  
                                                     Tuple2<String, String> t2 = new Tuple2<String, String>("ReferenceClinvarAssertion.TraitSet.Trait.XRef.DB", attl.getTextContent());
                                                     iter.add(t2);
                                                            System.out.println("|    |    |    |    |-- DB " + attl.getTextContent());//  + att.getTextContent());
                                                       }
                                                    }
                                       }

                                }

                          }
                         
                         

                     }
              
                     
                     // ClinVarAssertion processing
 
                     NodeList cnList = doc.getElementsByTagName("ClinVarAssertion");
                     Node node1 = cnList.item(0);
                     System.out.println("|-- ClinVarAssertion");
                     NamedNodeMap attscv = node1.getAttributes();
                     for (int i = 0; i < attscv.getLength(); i ++ ) {
                          Node att = attscv.item(i);
                          if (att.getNodeName().equals("DateCreated")) {
                                                     Tuple2<String, String> t2 = new Tuple2<String, String>("ClinvarAssertion.DateCreated", att.getTextContent());
                                                     iter.add(t2);
                              System.out.println("|    |-- DateCreated " + att.getTextContent());
                          }
                          if (att.getNodeName().equals("DateLastUpdated")) {
                                                     Tuple2<String, String> t2 = new Tuple2<String, String>("ClinvarAssertion.DateLastUpdated", att.getTextContent());
                                                     iter.add(t2);
                              System.out.println("|    |-- DateLastUpdated " + att.getTextContent());
                          }
                          if (att.getNodeName().equals("ID")) {
                                                     Tuple2<String, String> t2 = new Tuple2<String, String>("ClinvarAssertion.ID", att.getTextContent());
                                                     iter.add(t2);
                              System.out.println("|    |-- ID " + att.getTextContent());
                          }
                     }

                     Element e1 = (Element) node1;
                     System.out.println("|    |-- Assertion: " + e1.getElementsByTagName("Assertion").item(0).getTextContent());
                                                     Tuple2<String, String> t2 = new Tuple2<String, String>("ClinvarAssertion.Assertion", e1.getElementsByTagName("Assertion").item(0).getTextContent());
                                                     iter.add(t2);

                     // CV_attributeSet
                     NodeList attributeSetlist = e1.getElementsByTagName("AttributeSet");
                     
                     for (int i =0 ; i<attributeSetlist.getLength(); i++) {
                          if (attributeSetlist.item(i).getNodeName().equals("AttributeSet")) {
                             NodeList att = attributeSetlist.item(i).getChildNodes();
                             for (int j=0; j< att.getLength(); j++) {
                                 Node attchild = att.item(j);
                                 if (attchild.getNodeName().equals("Attribute")) {
                                                     Tuple2<String, String> t2 = new Tuple2<String, String>("ClinvarAssertion.AttributeSet.Attribute", attchild.getTextContent());
                                                     iter.add(t2);
                                    System.out.println("|    |    |    |-- Attribute " + attchild.getTextContent());
                                 }
                                 if (attchild.getNodeName().equals("Citation")) {
                                    System.out.println("|    |    |    |-- Citation ");//  + att.getTextContent());
                                    NodeList attlistn = attchild.getChildNodes();
                                    for (int m=0; m<attlistn.getLength(); m++) {
                                        Node name = attlistn.item(m);
                                        if (name.getNodeName().equals("Abbrev")) {
                                                     Tuple2<String, String> t2 = new Tuple2<String, String>("ClinvarAssertion.AttributeSet.Citation.Abbrev", name.getTextContent());
                                                     iter.add(t2);
                                            System.out.println("|    |    |    |    |-- Abbrev "  + name.getTextContent());
                                        }
                                        if (name.getNodeName().equals("ID")) {
                                                     Tuple2<String, String> t2 = new Tuple2<String, String>("ClinvarAssertion.AttributeSet.Citation.ID", name.getTextContent());
                                                     iter.add(t2);
                                            System.out.println("|    |    |    |    |-- ID "  + name.getTextContent());
                                        }
                                        if (name.getNodeName().equals("CitationText")) {
                                                     Tuple2<String, String> t2 = new Tuple2<String, String>("ClinvarAssertion.AttributeSet.Citation.CitationText", name.getTextContent());
                                                     iter.add(t2);
                                            System.out.println("|    |    |    |    |-- CitationText "  + name.getTextContent());
                                        }
                                        if (name.getNodeName().equals("URL")) {
                                                     Tuple2<String, String> t2 = new Tuple2<String, String>("ClinvarAssertion.AttributeSet.Citation.URL", name.getTextContent());
                                                     iter.add(t2);
                                            System.out.println("|    |    |    |    |-- URL "  + name.getTextContent());
                                        }

                                    }
                                 }
    
                             }
                         }     
                     }

                     Node cvsub = e1.getElementsByTagName("ClinVarSubmissionID").item(0);
                     System.out.println("|    |-- ClinVarSubmissionID ");
                     if (cvsub.getAttributes().item(0).getNodeName().equals("localKey")) {
                                                     Tuple2<String, String> t2 = new Tuple2<String, String>("ClinvarAssertion.ClinVarSubmissionID.localKey", cvsub.getAttributes().item(0).getTextContent());
                                                     iter.add(t2);
                          System.out.println("|    |    |-- localKey" + cvsub.getAttributes().item(0).getTextContent()); 
                     }
                     if (cvsub.getAttributes().item(1).getNodeName().equals("submitter")) {
                                                     Tuple2<String, String> t2 = new Tuple2<String, String>("ClinvarAssertion.ClinVarSubmissionID.submitter", cvsub.getAttributes().item(1).getTextContent());
                                                     iter.add(t2);
                          System.out.println("|    |    |-- submitter" + cvsub.getAttributes().item(1).getTextContent()); 
                     }
                     
                     
                     NodeList ClinVar_citation = e1.getElementsByTagName("Citation");
                     if (ClinVar_citation.getLength() == 0) {
                          System.out.println("|    |-- Citation:  null");
                          //System.out.println("ReferenceClinVarAssertion.Citation: null");
                     }
                     String ClinVar_ClinVarAccession = e1.getElementsByTagName("ClinVarAccession").item(0).getTextContent();
                                                     Tuple2<String, String> t2 = new Tuple2<String, String>("ClinvarAssertion.ClinVarAccession", ClinVar_ClinVarAccession);
                                                     iter.add(t2);
                     System.out.println("|    |-- ClinVarAccession: "+  ClinVar_ClinVarAccession ); 

                     // clinical_significance
                     Node ClinVar_ClinicalSignificance = e1.getElementsByTagName("ClinicalSignificance").item(0);


                     System.out.println("|    |-- ClinicalSignificance " ); 

                     NamedNodeMap csattscv = ClinVar_ClinicalSignificance.getAttributes();
                     if(csattscv.item(0).getNodeName().equals("DateLastEvaluated")) {
                           System.out.println("|    |    |-- DateLastEvaluated: " + csattscv.item(0).getTextContent());
                     }
 
                     if (! ClinVar_ClinicalSignificance.hasChildNodes()) {
                           System.out.println(" significance no child ");
                     }
                     NodeList listcv = ClinVar_ClinicalSignificance.getChildNodes();
                     for (int i =0 ; i< listcv.getLength(); i++) {
                          Node subnode = listcv.item(i);
                          if (subnode.getNodeName().equals("Description")) {
                                                     Tuple2<String, String> t2 = new Tuple2<String, String>("ClinvarAssertion.ClinicalSignificance.Description", subnode.getTextContent());
                                                     iter.add(t2);
                               System.out.println("|    |    |-- Description: " + subnode.getTextContent());
                          }
                          if (subnode.getNodeName().equals("ReviewStatus")) {
                                                     Tuple2<String, String> t2 = new Tuple2<String, String>("ClinvarAssertion.ClinicalSignificance.ReviewStatus", subnode.getTextContent());
                                                     iter.add(t2);
                               System.out.println("|    |    |-- ReviewStatus: " + subnode.getTextContent());
                          }
                          
                          if (subnode.getNodeName().equals("Explanation")) {
                                                     Tuple2<String, String> t2 = new Tuple2<String, String>("ClinvarAssertion.ClinicalSignificance.Explanation", subnode.getTextContent());
                                                     iter.add(t2);
                               System.out.println("|    |    |-- Explanation: " + subnode.getTextContent());
                          }
                          
                         
                     }

                     
                     //GenotypeSet

                     Node CV_GenotypeSet = e1.getElementsByTagName("GenotypeSet").item(0);


                     System.out.println("|    |-- GenotypeSet:");
                     NamedNodeMap genoattscv = CV_GenotypeSet.getAttributes();
                     if(genoattscv.item(0).getNodeName().equals("ID")) {
                           System.out.println("|    |    |-- ID: " + genoattscv.item(0).getTextContent());
                     }   

                     if(genoattscv.item(1).getNodeName().equals("Type")) {
                                                     Tuple2<String, String> t2 = new Tuple2<String, String>("ClinvarAssertion.GenotypeSet.Type",  genoattscv.item(1).getTextContent());
                                                     iter.add(t2);
                           System.out.println("|    |    |-- Type: " + genoattscv.item(1).getTextContent());
                     }   
 
                     if (! CV_GenotypeSet.hasChildNodes()) {
                           System.out.println(" genotype no child "); 
                     }   
                     NodeList genolistcv = CV_GenotypeSet.getChildNodes();

                     //System.out.println("genolist Length  " + genolist.getLength());

                     for (int i =0 ; i< genolistcv.getLength(); i++) {
                          Node subnode = genolistcv.item(i);
                          //System.out.println("i  [" +  i + "] " + "Name " +  subnode.getNodeName());


                          if (subnode.getNodeName().equals("Name")) {
                               System.out.println("|    |    |-- Name: ");// + subnode.getTextContent());
                               NodeList attlist = subnode.getChildNodes();
                               for (int k=0; k<attlist.getLength(); k++) {
                                      Node att = attlist.item(k);
                                      if (att.getNodeName().equals("ElementValue")) {
                                                     Tuple2<String, String> t2 = new Tuple2<String, String>("ClinvarAssertion.GenotypeSet.Name.ElementValue",  att.getTextContent());
                                                     iter.add(t2);
                                               System.out.println("|    |    |    | -- ElementValue "  + att.getTextContent());
                                       }
                                }

                          }   

                          // rcv__genotypeset__measureset
                          if (subnode.getNodeName().equals("MeasureSet")) {
                           
                               ref_sum = ref_sum + 200;

                               System.out.println("|    |    |-- MeasureSet: ");
                               NamedNodeMap msatts = subnode.getAttributes();
                               for (int j = 0; j < msatts.getLength(); j ++ ) {
                                  Node att = msatts.item(j);
                                  if (att.getNodeName().equals("ID")) {
                                                     Tuple2<String, String> t2 = new Tuple2<String, String>("ClinvarAssertion.GenotypeSet.MeasureSet.ID",  att.getTextContent());
                                                     iter.add(t2);
                                      System.out.println("|    |    |    |-- ID " + att.getTextContent());
                                   }
                                  if (att.getNodeName().equals("Type")) {
                                                     Tuple2<String, String> t2 = new Tuple2<String, String>("ClinvarAssertion.GenotypeSet.MeasureSet.Type",  att.getTextContent());
                                                     iter.add(t2);
                                      System.out.println("|    |    |    |-- Type " + att.getTextContent());
                                   }
                                }

                               
                               NodeList mslist = subnode.getChildNodes();
                               for (int j=0; j < mslist.getLength(); j++) {
                                  Node ssubnode = mslist.item(j);
              
                                  if (ssubnode.getNodeName().equals("Name")) {

                                      System.out.println("|    |    |    |-- Name ");//  + ssubnode.getTextContent());

                                      NodeList attlist = ssubnode.getChildNodes();
                                      for (int k=0; k<attlist.getLength(); k++) {
                                         Node att = attlist.item(k);
                                         if (att.getNodeName().equals("ElementValue")) {
                                               System.out.println("|    |    |    |    |    |-- ElementValue "  + att.getTextContent());
                                         }

                                         if (att.getNodeName().equals("XRef")) {
                                               System.out.println("|    |    |    |    |    |-- XRef "  + att.getTextContent());
                                         }
                                      }
                                  }
                                      
                                  if (ssubnode.getNodeName().equals("AttributeSet")) {
                                      System.out.println("|    |    |    | -- AttributeSet ");//  + ssubnode.getTextContent());
                                      NodeList attlist = ssubnode.getChildNodes();
                                      for (int k=0; k<attlist.getLength(); k++) {
                                           Node att = attlist.item(k);
                                           if (att.getNodeName().equals("Attribute")) { 
                                               System.out.println("|    |    |    |    |-- Attribute "  + att.getTextContent());
                                           }
                                      }
                                      
                                  }

                                  if (ssubnode.getNodeName().equals("Measure")) {


                                      System.out.println("|    |    |    |-- Measure " );

                                      NamedNodeMap matts = ssubnode.getAttributes();
                                      for (int k = 0; k < matts.getLength(); k ++ ) {
                                         Node att = matts.item(k);
                                         if (att.getNodeName().equals("ID")) {
                                             System.out.println("|    |    |    |    |-- ID " + att.getTextContent());
                                         }
                                         if (att.getNodeName().equals("Type")) {
                                             System.out.println("|    |    |    |    |-- Type " + att.getTextContent());
                                         }
                                      }

                                      NodeList ssubnodelist = ssubnode.getChildNodes();

                                      for (int k = 0; k < ssubnodelist.getLength(); k++) {
                                         Node sssubnode = ssubnodelist.item(k);

                                         // ref_genotypset_mset_measure_name
                                         if (sssubnode.getNodeName().equals("Name")) {

                                             System.out.println("|    |    |    |    |-- Name ");// + sssubnode.getTextContent());

                                             NodeList attlist = sssubnode.getChildNodes();
                                             for (int m=0; m<attlist.getLength(); m++) {
                                                Node att = attlist.item(m);
                                                if (att.getNodeName().equals("ElementValue")) {
                                                    System.out.println("|    |    |    |    |    |-- ElementValue "  + att.getTextContent());
                                                }

                                             }

                                         }

                                         // ref_genotypeset_mset_measure_sequenceLocation
                                         if (sssubnode.getNodeName().equals("SequenceLocation")) {
                                             System.out.println("|    |    |    |    |-- SequenceLocation ");// + sssubnode.getTextContent());
                                             NamedNodeMap sq = sssubnode.getAttributes();
                                             for (int m = 0; m < sq.getLength(); m++) {
                                                  Node att = sq.item(m);
                                                  if (att.getNodeName().equals("Assembly")) {
                                                      System.out.println("|    |    |    |    |    |-- Assembly: " + att.getTextContent());// + sssubnode.getTextContent());
                                                  }
                                                  if (att.getNodeName().equals("AssemblyAccessionVersion")) {
                                                      System.out.println("|    |    |    |    |    |-- AssemblyAccessVersion: " + att.getTextContent());// + sssubnode.getTextContent());
                                                  }
                                                  if (att.getNodeName().equals("AssemblyStatus")) {
                                                      System.out.println("|    |    |    |    |    |-- AssemblyStatus: " + att.getTextContent());// + sssubnode.getTextContent());
                                                  }
                                                  if (att.getNodeName().equals("Chr")) {
                                                      System.out.println("|    |    |    |    |    |-- Chr: " + att.getTextContent());// + sssubnode.getTextContent());
                                                  }
                                                  if (att.getNodeName().equals("Accession")) {
                                                      System.out.println("|    |    |    |    |    |-- Accession: " + att.getTextContent());// + sssubnode.getTextContent());
                                                  }
                                                  if (att.getNodeName().equals("start")) {
                                                      System.out.println("|    |    |    |    |    |-- start: " + att.getTextContent());// + sssubnode.getTextContent());
                                                  }
                                                  if (att.getNodeName().equals("stop")) {
                                                      System.out.println("|    |    |    |    |    |-- stop: " + att.getTextContent());// + sssubnode.getTextContent());
                                                  }
                                                  if (att.getNodeName().equals("display_start")) {
                                                      System.out.println("|    |    |    |    |    |-- display_start: " + att.getTextContent());// + sssubnode.getTextContent());
                                                  }
                                                  if (att.getNodeName().equals("variantLength")) {
                                                      System.out.println("|    |    |    |    |    |-- variantLength: " + att.getTextContent());// + sssubnode.getTextContent());
                                                  }
                                                  if (att.getNodeName().equals("referenceAllele")) {
                                                      System.out.println("|    |    |    |    |    |-- referenceAllele: " + att.getTextContent());// + sssubnode.getTextContent());
                                                  }
                                                  if (att.getNodeName().equals("alternateAllele")) {
                                                      System.out.println("|    |    |    |    |    |-- alternateAllele: " + att.getTextContent());// + sssubnode.getTextContent());
                                                  }
                                                  if (att.getNodeName().equals("positionVCF")) {
                                                      System.out.println("|    |    |    |    |    |-- positionVCF: " + att.getTextContent());// + sssubnode.getTextContent());
                                                  }
                                                  if (att.getNodeName().equals("referenceAlleVCF")) {
                                                      System.out.println("|    |    |    |    |    |-- referenceAlleleVCF: " + att.getTextContent());// + sssubnode.getTextContent());
                                                  }
                                                  if (att.getNodeName().equals("alternateAlleleVCF")) {
                                                      System.out.println("|    |    |    |    |    |-- alternateAlleleVCF: " + att.getTextContent());// + sssubnode.getTextContent());
                                                  }
                                                  if (att.getNodeName().equals("Strand")) {
                                                      System.out.println("|    |    |    |    |    |-- Strand: " + att.getTextContent());// + sssubnode.getTextContent());
                                                  }
                                              }
                                            
                                         }

                                         // ref_genotypset_mset_measure_attributeSet
                                         if (sssubnode.getNodeName().equals("AttributeSet")) {
                                            System.out.println("|    |    |    |    |-- AttributeSet ");//  + ssubnode.getTextContent());
                                            NodeList attlist = sssubnode.getChildNodes();
                                            for (int m=0; m<attlist.getLength(); m++) {
                                                Node att = attlist.item(m);
                                                if (att.getNodeName().equals("Attribute")) { 
                                                    System.out.println("|    |    |    |    |    |-- Attribute "  + att.getTextContent());
                                                }
                                                if (att.getNodeName().equals("XRef")) { 
                                                    System.out.println("|    |    |    |    |    |-- XRef ");//  + att.getTextContent());
                                                    NamedNodeMap mattsa = att.getAttributes();
                                                    for (int h = 0; h < mattsa.getLength(); h++) {
                                                       Node attl= mattsa.item(h);
                                                       if (attl.getNodeName().equals("ID")) {
                                                            System.out.println("|    |    |    |    |    |    |-- ID " + attl.getTextContent());//  + att.getTextContent());
                                                       }
                                                       if (attl.getNodeName().equals("DB")) {
                                                            System.out.println("|    |    |    |    |    |    |-- DB " + attl.getTextContent());//  + att.getTextContent());
                                                       }
                                                    }
                                                }
                                            }
                                      
                                          } // end of ref_geno_mset_attributeset

                                         // ref_genotypset_mset_measure_measurerelationship
                                         if (sssubnode.getNodeName().equals("MeasureRelationship")) {


                                            System.out.println("|    |    |    |    |-- MeasureRelationship ");//  + ssubnode.getTextContent());
                                            NamedNodeMap mratt = sssubnode.getAttributes();
                                            if (mratt.item(0).getNodeName().equals("Type")) {
                                                System.out.println("|    |    |    |    |    |-- Type: " + mratt.item(0).getTextContent() );//  + ssubnode.getTextContent());
                                            }
  
    
                                            NodeList attlist = sssubnode.getChildNodes();
                                            for (int m=0; m<attlist.getLength(); m++) {
                                                Node att = attlist.item(m);

                                                 if (att.getNodeName().equals("Name")) {
                                                     System.out.println("|    |    |    |    |    |-- Name ");//  + ssubnode.getTextContent());

                                                     NodeList attlistname = att.getChildNodes();
                                                     for (int h=0; h<attlistname.getLength(); h++) {
                                                        Node attname = attlistname.item(h);
                                                        if (attname.getNodeName().equals("ElementValue")) {
                                                            System.out.println("|    |    |    |    |    |    |-- ElementValue "  + attname.getTextContent());
                                                         }

                                                     }
                                                 }
                                                 if (att.getNodeName().equals("Symbol")) {
                                                     System.out.println("|    |    |    |    |    |-- Symbol ");//  + ssubnode.getTextContent());

                                                     NodeList attlistname = att.getChildNodes();
                                                     for (int h=0; h<attlistname.getLength(); h++) {
                                                        Node attname = attlistname.item(h);
                                                        if (attname.getNodeName().equals("ElementValue")) {
                                                            System.out.println("|    |    |    |    |    |    |-- ElementValue "  + attname.getTextContent());
                                                         }

                                                     }
                                                 }



                                            }
                                      
                                          } // end of ref_geno_mset_attributeset

                                         

                                      
                                      }

                                      
                                   }
                                      
                                      
                                
                               }
                          }   

    
                     }   

                     // clinvarassertion-measureSet cleaned.
                     Node cv_MeasureSet = e1.getElementsByTagName("MeasureSet").item(0);
                      System.out.println("|    |-- MeasureSet");

                     NamedNodeMap refattcv = cv_MeasureSet.getAttributes();
                      if(refattcv.item(1).getNodeName().equals("ID")) {
                           System.out.println("|    |    |-- ID: " + refattcv.item(1).getTextContent());
                     }
                     if(refattcv.item(0).getNodeName().equals("Type")) {
                           System.out.println("|    |    |-- Type: " + refattcv.item(0).getTextContent());
                     }

                     
                     NodeList cvmlist = cv_MeasureSet.getChildNodes();
                      for (int i =0; i< cvmlist.getLength(); i++) {

                         Node att = cvmlist.item(i);

                         if (att.getNodeName().equals("Name")) {

                              System.out.println("|    |    |-- Name ");//  + att.getTextContent());
                              NodeList attlistn = att.getChildNodes();
                              for (int m=0; m<attlistn.getLength(); m++) {
                                   Node name = attlistn.item(m);
                                   if (name.getNodeName().equals("ElementValue")) {
                                        System.out.println("|    |    |    |    |-- ElementValue "  + name.getTextContent());
                                   }
                                   /*
                                   if (name.getNodeName().equals("XRef")) {
                                        System.out.println("|    |    |    |    |-- XRef ");//  + name.getTextContent());
                                        NamedNodeMap natt = name.getAttributes();
                                        for (int h = 0 ; h< natt.getLength(); h++) {
                                             if (natt.item(h).getNodeName().equals("ID")) {
                                                 System.out.println("|    |    |    |    |    |-- ID " + natt.item(h).getTextContent());//  + name.getTextContent());
                                             }
                                             if (natt.item(h).getNodeName().equals("DB")) {
                                                 System.out.println("|    |    |    |    |    |-- DB " + natt.item(h).getTextContent());//  + name.getTextContent());
                                             }
                                        }
                                    }
                                    */
                              }

                         }
                        if (att.getNodeName().equals("Citation")) {
                                             System.out.println("|    |    |    |-- Citation ");//  + att.getTextContent());
                                             
                                             NamedNodeMap cv_mset_citationlist = att.getAttributes();
                                             if (cv_mset_citationlist.item(0).getNodeName().equals("Type")) {
                                                    System.out.println("|    |    |    |    |-- Type "  + cv_mset_citationlist.item(0).getTextContent());
                                             }

                                             NodeList attlistn = att.getChildNodes();
                                             for (int m=0; m<attlistn.getLength(); m++) {
                                                Node name = attlistn.item(m);
                                                if (name.getNodeName().equals("Abbrev")) {
                                                    System.out.println("|    |    |    |    |-- Abbrev "  + name.getTextContent());
                                                }
                                                if (name.getNodeName().equals("ID")) {
                                                    System.out.println("|    |    |    |    |-- ID "  + name.getTextContent());
                                                }
                                                if (name.getNodeName().equals("CitationText")) {
                                                    System.out.println("|    |    |    |    |-- CitationText "  + name.getTextContent());
                                                }
                                                if (name.getNodeName().equals("URL")) {
                                                    System.out.println("|    |    |    |    |-- URL "  + name.getTextContent());
                                                }
                       
                                             }
                         }


                         if (att.getNodeName().equals("AttributeSet")) {

                              System.out.println("|    |    |-- AttributeSet ");//  + att.getTextContent());
                              NodeList attlistn = att.getChildNodes();
                              for (int m=0; m<attlistn.getLength(); m++) {
                                   Node name = attlistn.item(m);
                                   if (name.getNodeName().equals("Attribute")) {
                                        System.out.println("|    |    |    |    |-- Attribute "  + name.getTextContent());
                                   }   
                                   /*
                                   if (name.getNodeName().equals("XRef")) {
                                        System.out.println("|    |    |    |    |-- XRef ");//  + name.getTextContent());
                                        NamedNodeMap natt = name.getAttributes();
                                        for (int h = 0 ; h< natt.getLength(); h++) {
                                             if (natt.item(h).getNodeName().equals("ID")) {
                                                 System.out.println("|    |    |    |    |    |-- ID " + natt.item(h).getTextContent());//  + name.getTextContent());
                                             }   
                                             if (natt.item(h).getNodeName().equals("DB")) {
                                                 System.out.println("|    |    |    |    |    |-- DB " + natt.item(h).getTextContent());//  + name.getTextContent());
                                             }   
                                        }   
                                    }   
                                    */

                              }   

                         }   

                         // MeasureSet_measure
                          if (att.getNodeName().equals("Measure")) {


                                      System.out.println("|    |    |    |-- Measure " );

                                      NamedNodeMap matts = att.getAttributes();
                                      for (int k = 0; k < matts.getLength(); k ++ ) {
                                         Node attms = matts.item(k);
                                         if (attms.getNodeName().equals("ID")) {
                                             System.out.println("|    |    |    |    |-- ID " + attms.getTextContent());
                                         }
                                         if (attms.getNodeName().equals("Type")) {
                                             System.out.println("|    |    |    |    |-- Type " + attms.getTextContent());
                                         }
                                      }

                                      NodeList ssubnodelist = att.getChildNodes();

                                      for (int k = 0; k < ssubnodelist.getLength(); k++) {
                                         Node sssubnode = ssubnodelist.item(k);

                                         // ref_genotypset_mset_measure_name
                                         if (sssubnode.getNodeName().equals("Name")) {

                                             System.out.println("|    |    |    |    |-- Name ");// + sssubnode.getTextContent());

                                             NodeList attlist = sssubnode.getChildNodes();
                                             for (int m=0; m<attlist.getLength(); m++) {
                                                Node attms = attlist.item(m);
                                                if (attms.getNodeName().equals("ElementValue")) {
                                                    System.out.println("|    |    |    |    |    |-- ElementValue "  + attms.getTextContent());
                                                }

                                                if (attms.getNodeName().equals("XRef")) {
                                                     System.out.println("|    |    |    |    |    |-- XRef "  + attms.getTextContent());
                                                }
                                             }

                                         }


                                          if (att.getNodeName().equals("Citation")) {
                                             System.out.println("|    |    |    |-- Citation ");//  + att.getTextContent());
                                             
                                             NamedNodeMap cv_mset_citationlist = att.getAttributes();
                                             if (cv_mset_citationlist.item(0).getNodeName().equals("Type")) {
                                                    System.out.println("|    |    |    |    |-- Type "  + cv_mset_citationlist.item(0).getTextContent());
                                             }

                                             NodeList attlistn = att.getChildNodes();
                                             for (int m=0; m<attlistn.getLength(); m++) {
                                                Node name = attlistn.item(m);
                                                if (name.getNodeName().equals("Abbrev")) {
                                                    System.out.println("|    |    |    |    |-- Abbrev "  + name.getTextContent());
                                                }
                                                if (name.getNodeName().equals("ID")) {
                                                    System.out.println("|    |    |    |    |-- ID "  + name.getTextContent());
                                                }
                                                if (name.getNodeName().equals("CitationText")) {
                                                    System.out.println("|    |    |    |    |-- CitationText "  + name.getTextContent());
                                                }
                                                if (name.getNodeName().equals("URL")) {
                                                    System.out.println("|    |    |    |    |-- URL "  + name.getTextContent());
                                                }
                       
                                             }
                                         }


                                         // ref_mset_measure_sequenceLocation
                                         if (sssubnode.getNodeName().equals("SequenceLocation")) {
                                             System.out.println("|    |    |    |    |-- SequenceLocation ");// + sssubnode.getTextContent());
                                             NamedNodeMap sq = sssubnode.getAttributes();
                                             for (int m = 0; m < sq.getLength(); m++) {
                                                  Node attms = sq.item(m);
                                                  if (attms.getNodeName().equals("Assembly")) {
                                                      System.out.println("|    |    |    |    |    |-- Assembly: " + attms.getTextContent());// + sssubnode.getTextContent());
                                                  }
                                                  if (attms.getNodeName().equals("AssemblyAccessionVersion")) {
                                                      System.out.println("|    |    |    |    |    |-- AssemblyAccessVersion: " + attms.getTextContent());// + sssubnode.getTextContent());
                                                  }
                                                  if (attms.getNodeName().equals("AssemblyStatus")) {
                                                      System.out.println("|    |    |    |    |    |-- AssemblyStatus: " + attms.getTextContent());// + sssubnode.getTextContent());
                                                  }
                                                  if (attms.getNodeName().equals("Chr")) {
                                                      System.out.println("|    |    |    |    |    |-- Chr: " + attms.getTextContent());// + sssubnode.getTextContent());
                                                  }
                                                  if (attms.getNodeName().equals("Accession")) {
                                                      System.out.println("|    |    |    |    |    |-- Accession: " + attms.getTextContent());// + sssubnode.getTextContent());
                                                  }
                                                  if (attms.getNodeName().equals("start")) {
                                                      System.out.println("|    |    |    |    |    |-- start: " + attms.getTextContent());// + sssubnode.getTextContent());
                                                  }
                                                  if (attms.getNodeName().equals("stop")) {
                                                      System.out.println("|    |    |    |    |    |-- stop: " + attms.getTextContent());// + sssubnode.getTextContent());
                                                  }
                                                  if (attms.getNodeName().equals("display_start")) {
                                                      System.out.println("|    |    |    |    |    |-- display_start: " + attms.getTextContent());// + sssubnode.getTextContent());
                                                  }
                                                  if (attms.getNodeName().equals("variantLength")) {
                                                      System.out.println("|    |    |    |    |    |-- variantLength: " + attms.getTextContent());// + sssubnode.getTextContent());
                                                  }
                                                  if (attms.getNodeName().equals("referenceAllele")) {
                                                      System.out.println("|    |    |    |    |    |-- referenceAllele: " + attms.getTextContent());// + sssubnode.getTextContent());
                                                  }
                                                  if (attms.getNodeName().equals("alternateAllele")) {
                                                      System.out.println("|    |    |    |    |    |-- alternateAllele: " + attms.getTextContent());// + sssubnode.getTextContent());
                                                  }
                                                  if (attms.getNodeName().equals("positionVCF")) {
                                                      System.out.println("|    |    |    |    |    |-- positionVCF: " + attms.getTextContent());// + sssubnode.getTextContent());
                                                  }
                                                  if (attms.getNodeName().equals("referenceAlleVCF")) {
                                                      System.out.println("|    |    |    |    |    |-- referenceAlleleVCF: " + attms.getTextContent());// + sssubnode.getTextContent());
                                                  }
                                                  if (attms.getNodeName().equals("alternateAlleleVCF")) {
                                                      System.out.println("|    |    |    |    |    |-- alternateAlleleVCF: " + attms.getTextContent());// + sssubnode.getTextContent());
                                                  }
                                                  if (attms.getNodeName().equals("Strand")) {
                                                      System.out.println("|    |    |    |    |    |-- Strand: " + attms.getTextContent());// + sssubnode.getTextContent());
                                                  }
                                              }

                                         }
                                         // ref_Mset_measure_attributeSet
                                         if (sssubnode.getNodeName().equals("AttributeSet")) {
                                            System.out.println("|    |    |    |    |-- AttributeSet ");//  + ssubnode.getTextContent());
                                            NodeList attlist = sssubnode.getChildNodes();
                                            for (int m=0; m<attlist.getLength(); m++) {
                                                Node attms = attlist.item(m);
                                                if (attms.getNodeName().equals("Attribute")) {
                                                    System.out.println("|    |    |    |    |    |-- Attribute "  + attms.getTextContent());
                                                }
                                            }

                                          } // end of ref__mset_measure_attributeset
                                          // ref_genotypset_mset_measure_measurerelationship
                                         if (sssubnode.getNodeName().equals("MeasureRelationship")) {
                                             ref_sum = ref_sum + 200;

                                            System.out.println("|    |    |    |    |-- MeasureRelationship ");//  + ssubnode.getTextContent());
                                            NamedNodeMap mratt = sssubnode.getAttributes();
                                            if (mratt.item(0).getNodeName().equals("Type")) {
                                                System.out.println("|    |    |    |    |    |-- Type: " + mratt.item(0).getTextContent() );//  + ssubnode.getTextContent());
                                            }


                                            NodeList attlist = sssubnode.getChildNodes();
                                            for (int m=0; m<attlist.getLength(); m++) {
                                                Node attms = attlist.item(m);

                                                 if (attms.getNodeName().equals("Name")) {
                                                     System.out.println("|    |    |    |    |    |-- Name ");//  + attms.getTextContent());

                                                     NodeList attlistname = attms.getChildNodes();
                                                     for (int h=0; h<attlistname.getLength(); h++) {
                                                        Node attname = attlistname.item(h);
                                                        if (attname.getNodeName().equals("ElementValue")) {
                                                            System.out.println("|    |    |    |    |    |    |-- ElementValue "  + attname.getTextContent());
                                                         }

                                                     }
                                                 }

                                                 if (attms.getNodeName().equals("SequenceLocation")) {
                                                      System.out.println("|    |    |    |    |    |-- SequenceLocation ");// + sssubnode.getTextContent());
                                                      NamedNodeMap sq = att.getAttributes();
                                                      for (int h = 0; h < sq.getLength(); h++) {
                                                          Node ttt = sq.item(h);
                                                          if (ttt.getNodeName().equals("Assembly")) {
                                                            System.out.println("|    |    |    |    |    |    |-- Assembly: " + ttt.getTextContent());// + sssubnode.getTextContent());
                                                          }
                                                          if (ttt.getNodeName().equals("AssemblyAccessionVersion")) {
                                                             System.out.println("|    |    |    |    |    |    |-- AssemblyAccessVersion: " + ttt.getTextContent());// + sssubnode.getTextContent());
                                                          }
                                                          if (ttt.getNodeName().equals("AssemblyStatus")) {
                                                             System.out.println("|    |    |    |    |    |    |-- AssemblyStatus: " + ttt.getTextContent());// + sssubnode.getTextContent());
                                                          }
                                                          if (ttt.getNodeName().equals("Chr")) {
                                                             System.out.println("|    |    |    |    |    |    |-- Chr: " + ttt.getTextContent());// + sssubnode.getTextContent());
                                                          }
                                                          if (ttt.getNodeName().equals("Accession")) {
                                                             System.out.println("|    |    |    |    |    |    |-- Accession: " + ttt.getTextContent());// + sssubnode.getTextContent());
                                                          }
                                                          if (ttt.getNodeName().equals("start")) {
                                                              System.out.println("|    |    |    |    |    |    |-- start: " + ttt.getTextContent());// + sssubnode.getTextContent());
                                                          }
                                                          if (ttt.getNodeName().equals("stop")) {
                                                              System.out.println("|    |    |    |    |    |    |-- stop: " + ttt.getTextContent());// + sssubnode.getTextContent());
                                                          }
                                                          if (ttt.getNodeName().equals("display_start")) {
                                                              System.out.println("|    |    |    |    |    |    |-- display_start: " + ttt.getTextContent());// + sssubnode.getTextContent());
                                                          }
                                                          if (ttt.getNodeName().equals("variantLength")) {
                                                              System.out.println("|    |    |    |    |    |    |-- variantLength: " + ttt.getTextContent());// + sssubnode.getTextContent());
                                                          }
                                                          if (ttt.getNodeName().equals("referenceAllele")) {
                                                              System.out.println("|    |    |    |    |    |    |-- referenceAllele: " + ttt.getTextContent());// + sssubnode.getTextContent());
                                                          }
                                                          if (ttt.getNodeName().equals("alternateAllele")) {
                                                              System.out.println("|    |    |    |    |    |    |-- alternateAllele: " + ttt.getTextContent());// + sssubnode.getTextContent());
                                                          }
                                                          if (ttt.getNodeName().equals("positionVCF")) {
                                                              System.out.println("|    |    |    |    |    |    |-- positionVCF: " + ttt.getTextContent());// + sssubnode.getTextContent());
                                                          }
                                                          if (ttt.getNodeName().equals("referenceAlleVCF")) {
                                                              System.out.println("|    |    |    |    |    |    |-- referenceAlleleVCF: " + ttt.getTextContent());// + sssubnode.getTextContent());
                                                          }
                                                          if (ttt.getNodeName().equals("alternateAlleleVCF")) {
                                                              System.out.println("|    |    |    |    |    |    |-- alternateAlleleVCF: " + ttt.getTextContent());// + sssubnode.getTextContent());
                                                          }
                                                          if (ttt.getNodeName().equals("Strand")) {
                                                              System.out.println("|    |    |    |    |    |    |-- Strand: " + ttt.getTextContent());// + sssubnode.getTextContent());
                                                          }
                                                      }
                                                }


                                            }

                                          } // end of ref_geno_mset_attributeset




                        
                         }
                        }


                      }
                      
                      // process ref_observedINset
                      Node cv_ObservedInSet = e1.getElementsByTagName("ObservedIn").item(0);
            
                      System.out.println("|    |-- ObervedIn");
                      NodeList cv_oblist = cv_ObservedInSet.getChildNodes();
                       if (cv_oblist.getLength() > 1 ) {
                       ref_sum = ref_sum + 1000;
   //    System.out.println(" ref_sum " + ref_sum);
                       }
                      for (int i =0 ; i< cv_oblist.getLength(); i++) {
                          Node subnode = cv_oblist.item(i);
                           if (subnode.getNodeName().equals("Method")) {
                               System.out.println("|    |    |-- Method: ");// + subnode.getTextContent());
                               NodeList attlist = subnode.getChildNodes();
                               for (int k=0; k<attlist.getLength(); k++) {
                                      Node att = attlist.item(k);
                                      if (att.getNodeName().equals("MethodType")) {
                                               System.out.println("|    |    |    |-- MethodType "  + att.getTextContent());
                                       }
                                      if (att.getNodeName().equals("NamePlatform")) {
                                               System.out.println("|    |    |    |-- NamePlatform "  + att.getTextContent());
                                       }
                                      if (att.getNodeName().equals("Purpose")) {
                                               System.out.println("|    |    |    |-- Purpose "  + att.getTextContent());
                                       }
                                      if (att.getNodeName().equals("ResultType")) {
                                               System.out.println("|    |    |    |-- ResultType "  + att.getTextContent());
                                       }
                                      if (att.getNodeName().equals("Description")) {
                                               System.out.println("|    |    |    |-- Description "  + att.getTextContent());
                                       }
                                }

                          }
                           if (subnode.getNodeName().equals("Sample")) {
                               System.out.println("|    |    |-- Sample: ");// + subnode.getTextContent());
                               NodeList attlist = subnode.getChildNodes();
                               for (int k=0; k<attlist.getLength(); k++) {
                                      Node att = attlist.item(k);
                                      if (att.getNodeName().equals("AffectedStatuse")) {
                                               System.out.println("|    |    |    |-- AffectedStatus "  + att.getTextContent());
                                       }
                                      if (att.getNodeName().equals("Ethnicity")) {
                                               System.out.println("|    |    |    |-- Ethnicity "  + att.getTextContent());
                                       }
                                      if (att.getNodeName().equals("FamilyData")) {
                                               System.out.println("|    |    |    |-- FamilyData "  + att.getTextContent());
                                       }
                                      if (att.getNodeName().equals("NumberFemales")) {
                                               System.out.println("|    |    |    |-- NumberFemales "  + att.getTextContent());
                                       }
                                      if (att.getNodeName().equals("NumberMalesa")) {
                                               System.out.println("|    |    |    |-- NumberMales "  + att.getTextContent());
                                       }
                                      if (att.getNodeName().equals("NumberTested")) {
                                               System.out.println("|    |    |    |-- NumberTested "  + att.getTextContent());
                                       }
                                      if (att.getNodeName().equals("Origin")) {
                                               System.out.println("|    |    |    |-- Origin "  + att.getTextContent());
                                       }
                                      if (att.getNodeName().equals("Species")) {
                                               System.out.println("|    |    |    |-- Species "  + att.getTextContent());
                                      }
                               }
                           }
                        
                           // ObservedIn_ObservedData 
                           if (subnode.getNodeName().equals("ObservedData")) {
                               System.out.println("|    |    |-- ObservedData: ");// + subnode.getTextContent());
                               NamedNodeMap tstraitatts = subnode.getAttributes();
                               if(tstraitatts.item(0).getNodeName().equals("ID")) {
                                      System.out.println("|    |    |    |-- ID: " + tstraitatts.item(0).getTextContent());
                               }
                               if(tstraitatts.item(1).getNodeName().equals("Type")) {
                                   System.out.println("|    |    |-- Type: " + tstraitatts.item(1).getTextContent());
                               }


                               NodeList attlist = subnode.getChildNodes();

                               for (int j=0; j<attlist.getLength(); j++) {
                                      Node att = attlist.item(j);

                                      if (att.getNodeName().equals("Attribute")) {
                                          System.out.println("|    |    |    |-- Attribute: " + att.getTextContent());
                                      }

                                      if (att.getNodeName().equals("Citation")) {
                                               System.out.println("|    |    |    |-- Citation ");//  + att.getTextContent());
                                               NodeList attlistn = att.getChildNodes();
                                             for (int m=0; m<attlistn.getLength(); m++) {
                                                Node name = attlistn.item(m);
                                                if (name.getNodeName().equals("Abbrev")) {
                                                    System.out.println("|    |    |    |    |-- Abbrev "  + name.getTextContent());
                                                }
                                                if (name.getNodeName().equals("ID")) {
                                                    System.out.println("|    |    |    |    |-- ID "  + name.getTextContent());
                                                }
                                                if (name.getNodeName().equals("CitationText")) {
                                                    System.out.println("|    |    |    |    |-- CitationText "  + name.getTextContent());
                                                }
                                                if (name.getNodeName().equals("URL")) {
                                                    System.out.println("|    |    |    |    |-- URL "  + name.getTextContent());
                                                }

                                             }

                                      }
                              }
                      } 
                    }

                     // process ref_traitSet                  
              
                     Node cv_TraitSet = e1.getElementsByTagName("TraitSet").item(0);


                     System.out.println("|    |-- TraitSet:");
                     NamedNodeMap tsattscv = cv_TraitSet.getAttributes();
                     if(tsattscv.item(1).getNodeName().equals("ID")) {
                           System.out.println("|    |    |-- ID: " + tsattscv.item(1).getTextContent());
                     }
                     if(tsattscv.item(0).getNodeName().equals("Type")) {
                           System.out.println("|    |    |-- Type: " + tsattscv.item(0).getTextContent());
                     }


                     NodeList tslistcv = cv_TraitSet.getChildNodes();


                     for (int i =0 ; i< tslistcv.getLength(); i++) {
                          Node subnode = tslistcv.item(i);
                           if (subnode.getNodeName().equals("Comment")) {
                               System.out.println("|    |    |-- Comment: " + subnode.getTextContent());

                           }
                           // traitset_trait
                           if (subnode.getNodeName().equals("Trait")) {
                               System.out.println("|    |    |-- Trait: ");// + subnode.getTextContent());
                               NamedNodeMap tstraitatts = subnode.getAttributes();
                               if(tstraitatts.item(0).getNodeName().equals("ID")) {
                                      System.out.println("|    |    |    |-- ID: " + tstraitatts.item(0).getTextContent());
                               }
                               if(tstraitatts.item(1).getNodeName().equals("Type")) {
                                   System.out.println("|    |    |-- Type: " + tstraitatts.item(1).getTextContent());
                               }


                               NodeList attlist = subnode.getChildNodes();

                               for (int j=0; j<attlist.getLength(); j++) {
                                      Node att = attlist.item(j);

                                      if (att.getNodeName().equals("Name")) {
                                               System.out.println("|    |    |    |-- Name ");//  + att.getTextContent());
                                               NodeList attlistn = att.getChildNodes();
                                             for (int m=0; m<attlistn.getLength(); m++) {
                                                Node name = attlistn.item(m);
                                                if (name.getNodeName().equals("ElementValue")) {
                                                    System.out.println("|    |    |    |    |-- ElementValue "  + name.getTextContent());
                                                }
                                                if (name.getNodeName().equals("XRef")) {
                                                     System.out.println("|    |    |    |    |-- XRef ");//  + name.getTextContent());
                                                     NamedNodeMap natt = name.getAttributes();
                                                     for (int h = 0 ; h< natt.getLength(); h++) {
                                                       if (natt.item(h).getNodeName().equals("ID")) {
                                                           System.out.println("|    |    |    |    |    |-- ID " + natt.item(h).getTextContent());//  + name.getTextContent());
                                                       }
                                                       if (natt.item(h).getNodeName().equals("DB")) {
                                                           System.out.println("|    |    |    |    |    |-- DB " + natt.item(h).getTextContent());//  + name.getTextContent());
                                                       }
                                                     }
                                                }
                                             }
                                         
                                      }
                                      if (att.getNodeName().equals("Symbol")) {
                                               System.out.println("|    |    |    |-- Symbol ");//  + att.getTextContent());
                                               NodeList attlists = att.getChildNodes();
                                             for (int m=0; m<attlists.getLength(); m++) {
                                                Node name = attlists.item(m);
                                                if (name.getNodeName().equals("ElementValue")) {
                                                    System.out.println("|    |    |    |    |-- ElementValue "  + name.getTextContent());
                                                }
                                             }

                                      }
                                    if (att.getNodeName().equals("TraitRelationship")) {
                                            NamedNodeMap traitrelationlist = att.getAttributes();
                                            if (traitrelationlist.item(0).getNodeName().equals("ID")) {
                                               
                                              // map.put("clinvarassertion.TraitSet.Trait.ID", traitrelationlist.item(0).getTextContent());
                                                Tuple2<String, String> t2 = new Tuple2<String, String>("clinvarassertion.TraitSet.Trait.ID", traitrelationlist.item(0).getTextContent());
                                                iter.add(t2);
                                            }
                                            if (traitrelationlist.item(1).getNodeName().equals("Type")) {
                                               
                                                Tuple2<String, String> t2 = new Tuple2<String, String>("clinvarassertion.TraitSet.Trait.Type", traitrelationlist.item(1).getTextContent());
                                                iter.add(t2);
                                            }
                                            // add name and xref.

                                      }

                                      if (att.getNodeName().equals("XRef")) {
                                               System.out.println("|    |    |    |    |    |-- XRef ");//  + att.getTextContent());
                                               NamedNodeMap mattsa = att.getAttributes();   
                                               for (int h = 0; h < mattsa.getLength(); h++) {
                                                       Node attl= mattsa.item(h);
                                                       if (attl.getNodeName().equals("ID")) {
                                                            System.out.println("|    |    |    |    |    |    |-- ID " + attl.getTextContent());//  + att.getTextContent());
                                                       }         
                                                       if (attl.getNodeName().equals("DB")) {
                                                            System.out.println("|    |    |    |    |    |    |-- DB " + attl.getTextContent());//  + att.getTextContent());
                                                       }         
                                               }       
                                       }        




                                }

                          }
                         
                         

                     }

           
            csv.close();
            out.flush();
            out.close();
             return iter;
          } catch (Exception e) {
              ;
          } finally {
              ;
          }
            return iter;
  
        }}); 

        System.out.println(" NUM of hRDD " + hRDD.count());

       JavaRDD<Tuple2<String, String>> flatRTN =  hRDD.flatMap(
           new FlatMapFunction<ArrayList<Tuple2<String, String>>, Tuple2<String, String>>(){
                public Iterable<Tuple2<String, String>> call(ArrayList<Tuple2<String, String>> l){
                    return l;
                }
        });

       System.out.println(" NUM of flatRTN " + flatRTN.count());

       JavaPairRDD<String, String> matrixMapRDD = flatRTN.mapToPair(
            new PairFunction<Tuple2<String, String>, String, String>() {
                public Tuple2<String, String> call(Tuple2<String, String> t) {
                    System.out.println("key " + t._1 + " Value " + t._2);
                    return new Tuple2<String,String>(t._1, t._2);
                }
        });

       System.out.println(" NUM of matrixMapRDD " + matrixMapRDD.count());

      JavaPairRDD<String, Iterable<String>> distinctRDD = matrixMapRDD.groupByKey();

      //JavaRDD<Tuple2<String, String>> resultRDD = distinctRDD.map(
       //   new Function<Tuple2<String, Iterable<String>>, Tuple2<String, String>> () {
        //     public Tuple2<String, String> call(Tuple2<String, Iterable<String>> x) {
      JavaRDD<String>  resultRDD = distinctRDD.map(
          new Function<Tuple2<String, Iterable<String>>, String> () {
             public String call(Tuple2<String, Iterable<String>> x) {

                System.out.println("KEY " + x._1 + " Value " + x._2);

                 int maxlen = 1;
                 String maxStr="";

                 String secStr = "";
                 String thirdStr= "";
                 int cnt = 0;
                for (String str : x._2) {
                   if (cnt == 0) {
                      secStr = str;
                   }
                   if (cnt == 1) {
                      thirdStr = str;
                   } 
                   if ( str.length() > maxlen ) {
                         maxlen = str.length();
                         maxStr = str;
                   }
                   cnt ++;
                }        

               // Tuple2<String, String> t2 = new Tuple2<String, String> (x._1, maxStr + "||" + secStr + "||" + thirdStr);  
                //System.out.println("KEY " + t2._1 + " Value " + t2._2);
                return (x._1 + "  :::  " +  maxStr + "||" + secStr + "||" + thirdStr);  
         
    }});
     resultRDD.coalesce(1).saveAsTextFile("outputPath");
         
   }
}
         
