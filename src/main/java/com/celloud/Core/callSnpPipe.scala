package com.celloud.Core

import com.celloud.Utils.Constant
import org.apache.log4j.{Level, Logger}
import org.apache.spark.{SparkContext, SparkConf}

/**
  * Created by luo on 2017/3/22.
  */
object callSnpPipe {
  def LoadCallSnpPipe(isLocal: Boolean,partitionNum:Int,inputpath: String,samtoolsPath: String, outputpath: String): Unit ={
    Logger.getLogger("org").setLevel(Level.ERROR)
    val name = "callSnpPipe"
    val conf = new SparkConf().setAppName(name)
    if (isLocal) {
      conf.setMaster("local")
    }
    conf.set("spark.Kryoserializer.buffer.max", "8192m")
    val sc = new SparkContext(conf)

    val beginDate = System.currentTimeMillis()
    println("开始启动..................................................................................")
    //命令 /share/biosoft/Software/samtools-1.2/samtools mpileup__-f__/share/biosoft/Database/HG_19/hg19.fasta -
    val  csp =new callSnpPipe()
    val samtoolcommond = samtoolsPath+" -"
    println("------------samtoolcommond------------"+samtoolcommond)
    val disRDD = sc.textFile(inputpath).flatMap { v1line => v1line.split(Constant.linefeed) }.cache()
    disRDD.count()
    val titleRDD= disRDD.filter(x=> x.startsWith("@")).distinct().collect()

    var strBuf = new StringBuffer()
    for (i <- titleRDD.indices) {
      val lines = titleRDD(i)+ Constant.linefeed
      strBuf.append(lines)
    }
    val broadcastStr= sc.broadcast(strBuf.toString.substring(0,strBuf.length()-Constant.linefeed.length))
    val samtoolRDD2 = disRDD.filter(x=> !x.startsWith("@"))
      .filter(x=> x.split(Constant.tab)(2).toString!="*")
      .sortBy(_.split(Constant.tab)(3).toInt)
      .groupBy(_.split(Constant.tab)(2))
      .map{x=>
      val p = x._2.toIterator
      var strres = broadcastStr.value.toString
      while(p.hasNext){
        strres+= Constant.linefeed + p.next()
      }
      strres
    }.pipe(samtoolcommond).saveAsTextFile(outputpath)





//    val samtoolRDD = disRDD.filter(x=> !x.startsWith("@"))
//      .filter(x=> x.split(Constant.tab)(2).toString!="*")
//      .sortBy(_.split(Constant.tab)(3).toInt)
//      .groupBy(_.split(Constant.tab)(2))
//      .collect()
//    val size=samtoolRDD.length
//     var i=0
//     while(i<size){
//        sc.makeRDD(titleRDD).union(sc.makeRDD(samtoolRDD(i)._2.toList))
//          .coalesce(1).pipe(samtoolcommond).saveAsTextFile(outputpath+i)
//       i+=1
//     }

    disRDD.unpersist()
    //结束
    val endDate = System.currentTimeMillis()
    println("结束,计算的执行时间" + ((endDate - beginDate) / 1000) + "秒..........")
    sc.stop()
  }
}
class callSnpPipe extends Serializable{
   def callpipe(x:List[String],sc:SparkContext): Unit ={
     sc.makeRDD(x)
   }
}
