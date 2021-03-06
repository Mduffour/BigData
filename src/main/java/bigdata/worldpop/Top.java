package bigdata.worldpop;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.TreeMap;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;

  public class Top extends Configured implements Tool{
	  
	  public static class TopKMapper extends Mapper<LongWritable, Text, NullWritable, Text> {
		  public int k = 0;
		  
		  private TreeMap<LongWritable, Text> topKPop = new TreeMap<LongWritable, Text>();
		  
		  @Override
		  public void setup(Context context) {
			  Configuration conf = context.getConfiguration();
			  k = Integer.parseInt(conf.get("kPop"));
		  }
			  
		  public void map(LongWritable key, Text value, Context context) throws IOException, InterruptedException {
				if (key.get() == 0 ) 
					return;
				String tokens[] = value.toString().split(",");
				if (tokens.length < 7 || tokens[4].length()==0) return;
				int pop = 0;
				try {
					pop = Integer.parseInt(tokens[4]);
				}
				catch(Exception e) {				
					return;
				}
				topKPop.put(new LongWritable(pop), new Text(tokens[2]));
				if (topKPop.size() > k)
					topKPop.remove(topKPop.firstKey());
			}
		  protected void cleanup(Context context) throws IOException, InterruptedException {
			  for(Text t : topKPop.values()){
				  context.write(NullWritable.get(), t);
			  }
		  }
	  }
	  
	  public static class TopKCombiner extends Reducer<NullWritable, Text, NullWritable, Text> {
		  public int k = 0;
		  private TreeMap<LongWritable, Text> topKPop = new TreeMap<LongWritable, Text>();
		  public void setup(Context context) {
			  Configuration conf = context.getConfiguration();
			  k = Integer.parseInt(conf.get("kPop"));
		  	}
		  
		  public void reduce(NullWritable key, Iterable<Text> values, Context context) throws IOException,
		  				InterruptedException {
			  long cpt = 1;
			  for (Text value : values) {
				  topKPop.put(new LongWritable(cpt), new Text(value));
				  if (topKPop.size() > k)
					  topKPop.remove(topKPop.lastKey());
				  cpt++;
			  }
			  for(Text t : topKPop.values()){
				  context.write(key, t);
			  }
		  }
	  }
	 
		public static class TopKReducer extends Reducer<NullWritable, Text, IntWritable, Text> {
			  public int k = 0;
			  private TreeMap<LongWritable, Text> topKPop = new TreeMap<LongWritable, Text>();
			  public void setup(Context context) {
				  Configuration conf = context.getConfiguration();
				  k = Integer.parseInt(conf.get("kPop"));
			  	}
			  
			  public void reduce(NullWritable key, Iterable<Text> values, Context context) throws IOException,
			  				InterruptedException {
				  long cpt = 1;
				  for (Text value : values) {
					  topKPop.put(new LongWritable(cpt), new Text(value));
					  if (topKPop.size() > k)
						  topKPop.remove(topKPop.lastKey());
					  cpt++;
				  }
				  int a = 1;
				  for (Text t : topKPop.values()) {
					  context.write(new IntWritable(a), t);
					  a++;
				  }
			  }
		}
		  			
	  public int run(String[] args) throws IOException, ClassNotFoundException, InterruptedException {
	    Configuration conf = new Configuration();
	    Job job = Job.getInstance(conf, "TopK");
	    Path pOut = new Path("Result");
	    try {
	    	job.getConfiguration().set("kPop", args[0]);
		    FileInputFormat.addInputPath(job, new Path(args[1]));
		    FileOutputFormat.setOutputPath(job, pOut);
	    }
	    catch (Exception e)
	    {
	    	System.out.println(" bad arguments, waiting for 2 arguments [Integer] [inputURI]");
	    }
	    job.setNumReduceTasks(1);
	    job.setJarByClass(Top.class);
	    job.setMapperClass(TopKMapper.class);
	    job.setMapOutputKeyClass(NullWritable.class);
	    job.setMapOutputValueClass(Text.class);
	    job.setCombinerClass(TopKCombiner.class);
	    job.setReducerClass(TopKReducer.class);
	    job.setOutputKeyClass(IntWritable.class);
	    job.setOutputValueClass(Text.class);
	    job.setOutputFormatClass(TextOutputFormat.class);
	    int jobComplete = 1;
	    jobComplete = job.waitForCompletion(true) ? 0 : 1;
	    if(job.isSuccessful()){
	    	FileSystem fs = FileSystem.get(conf);
	    	Path p = new Path(FileOutputFormat.getOutputPath(job)+"/part-r-00000");
	    	FileStatus[] fss = fs.listStatus(p);
	        for (FileStatus status : fss) {
	            Path path = status.getPath();
	            //SequenceFile.Reader reader = new SequenceFile.Reader(conf, SequenceFile.Reader.file(path));
	            FSDataInputStream inputStream = fs.open(path);
	            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
	            String record;
	            System.out.println("\tResults");
	            while((record = reader.readLine()) != null) {
	                System.out.println("\t\t"+record);
	            }
	            reader.close();
	        }
	    }
	    return jobComplete;
	  }
	  
	  public static void main(String args[]) throws Exception {
			System.exit(ToolRunner.run(new Top(), args));
	  }
  }
  
	  
