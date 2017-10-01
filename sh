	rm -r out.txt
#/usr/local/spark/bin/spark-submit  --class XMLTextMining   target/xmlprocess-1.0.jar  file:/Users/hand10/Projects/MSSM/spark*/xml_spar*/inputfiles/med*.xml out.txt
	/Users/hand10/spark-1.6.2-bin-hadoop2.6/bin/spark-submit  --class mergesample.MergeSample   target/mergesample-1.0.jar  file:/Users/hand10/Projects/MSSM/BigData/jointsample_ByKey/inputfiles/  out.txt file:/Users/hand10/Projects/MSSM/BigData/jointsample_ByKey/input_backup/list.txt
	#/Users/hand10/spark-1.6.2-bin-hadoop2.6/bin/spark-submit  --class mergesample.MergeSample   target/mergesample-1.0.jar  file:/Users/hand10/Projects/MSSM/BigData/jointsample_ByKey/hello/  out.txt 
