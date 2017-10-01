
MergeSample Spark Implementation version 1.0

Description	In bioinformatics domain, we need to merge VCF files sometimes. There are 
		some code that can be used to this end. Such as VCFtools (Perl version).
		However, it is well known that Perl script usually runs very slowly with
		big files and it is not easy to implement parallelization. It is reported 
		that when the number of files is bigger than 1000, it is hard to use
		VCFtools for merging files. In reality, we often need to merge 10,000 huge
		files. So, under this situation, we have to seek other solution. Fortunatelly,
		Spark framework offers this opportunity. In this project, we use Spark to 
		merge VCF files efficiently.  
-------------------------------------------------------------------------------------------------------------
-------------------------------------------------------------------------------------------------------------

Dependency	We use Maven to handle the dependency issue. 
		The structure and components of pom.xml look as follows.
			<project>
 			   <modelVersion>4.0.0</modelVersion>
			   <!-- Information about the project -->
			   <groupId>com.databricks</groupId>
			   <artifactId>mergesample</artifactId>
  			   <name>Spark Project</name>
			   <packaging>jar</packaging>
			   <version>1.0</version>

			   <dependencies>

   			      <!-- spark dependency -->
 			      <dependency>
       			        <groupId>org.apache.spark</groupId>
       				<artifactId>spark-core_2.10</artifactId>
       				<version>1.6.0</version>
       				<scope>provided</scope>
    			      </dependency>
			      <!-- hadoop dependency -->
    			      <dependency>
     				<groupId>org.apache.hadoop</groupId>
      				<artifactId>hadoop-client</artifactId>
      				<version>2.5.0</version>
    			      </dependency>

			   <build>
     			     <plugins>
       			       <!-- Maven shade plug-in that creates uber JARS -->
       			       <plugin>
          			 <groupId>org.apache.maven.plugins</groupId>
          			 <artifactId>maven-shade-plugin</artifactId>
          			 <version>2.3</version>
          			 <executions>
            			   <execution>
              			     <phase>package</phase>
              			     <goals>
                		       <goal>shade</goal>
              		             </goals>
            			   </execution>
          			 </executions>

                                 <configuration>
                                   <filters>
                                     <filter>
                                       <artifact>*:*</artifact>
                                         <excludes>
                                         <exclude>META-INF/*.SF</exclude>
                                         <exclude>META-INF/*.DSA</exclude>
                                         <exclude>META-INF/*.RSA</exclude>
                                         </excludes>
                                     </filter>
                                   </filters>
                                 </configuration>
                               </plugin>
                             </plugins>
                           </build>
                         </project>


-------------------------------------------------------------------------------------------------------------
-------------------------------------------------------------------------------------------------------------

Usage		In order to use it, we strongly recommend to use spark-submit command
		to launch your job.

 		/Users/hand10/spark-1.6.2-bin-hadoop2.6/bin/spark-submit  --class mergesample.MergeSample \
		  target/mergesample-1.0.jar  file:input file:output 

-------------------------------------------------------------------------------------------------------------
-------------------------------------------------------------------------------------------------------------
Report Issue	Thank you for using our spark tool to merge your VCF files. If you have any questions or 
		you find any issues, please reach out to me at dianwei.han@sema4genomics.com
