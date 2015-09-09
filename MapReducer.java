import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.lang.Math;

import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.*;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.SequenceFileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.SequenceFileOutputFormat;
import org.apache.hadoop.util.GenericOptionsParser;

/*
 * This is the skeleton for CS61c project 1, Fall 2013.
 *
 * Reminder:  DO NOT SHARE CODE OR ALLOW ANOTHER STUDENT TO READ YOURS.
 * EVEN FOR DEBUGGING. THIS MEANS YOU.
 *
 */
public class Proj1{

    /*
     * Inputs is a set of (docID, document contents) pairs.
     */
    public static class Map1 extends Mapper<WritableComparable, Text, Text, DoublePair> {
        /** Regex pattern to find words (alphanumeric + _). */
        final static Pattern WORD_PATTERN = Pattern.compile("\\w+");

        private String targetGram = null;
        private int funcNum = 0;
        /** Text object to store a word to write to output. */
        private Text word = new Text();
        /** Text object to store a word to write to output. */
        private DoublePair total_and_fd = new DoublePair();
        /*
         * Setup gets called exactly once for each mapper, before map() gets called the first time.
         * It's a good place to do configuration or setup that can be shared across many calls to map
         */
        @Override
            public void setup(Context context) {
                targetGram = context.getConfiguration().get("targetWord").toLowerCase();
                try {
                    funcNum = Integer.parseInt(context.getConfiguration().get("funcNum"));
                } catch (NumberFormatException e) {
                    /* Do nothing. */
                }
            }

        @Override
            public void map(WritableComparable docID, Text docContents, Context context)
            throws IOException, InterruptedException {
                Matcher matcher = WORD_PATTERN.matcher(docContents.toString());
                Func func = funcFromNum(funcNum);

                // YOUR CODE HERE
                int i = 0;
                int j = 0; 
                List<Integer> target_index = new ArrayList<Integer>();
                
                while (matcher.find()) {
                    String word1 = matcher.group().toLowerCase();
                    if (word1.equals(targetGram)){
                        target_index.add(i);
                    } 
                    i++;
                }
                
                Matcher matcher2 = WORD_PATTERN.matcher(docContents.toString());
                i=0;
                while (matcher2.find()) { //while there are words in the document do this
                    Double d = new Double(Double.POSITIVE_INFINITY); //initialize the distance between words and the target word to infinity
                    String word2 = matcher2.group().toLowerCase();//word2 is the current word
                    if (!word2.equals(targetGram)){ //if the current word is not the target word do this
                        for (int x = 0; x < target_index.size(); x++){ //for each instance of the target word in the document
                            if ((Math.abs(i - target_index.get(x))) < d){ //if the distance between the current word indice and the target word indice is less than d
                                d = (double) Math.abs(i - target_index.get(x)); //set d to the distance between the current word and the target word instance 
                            }
                        }
                    word.set(word2); //make the current word a Text variable
                    total_and_fd.setDouble1(1); //set the total occurences of current word to double1 in DoublePair
                    total_and_fd.setDouble2(func.f(d)); //set double2 to f(d)
                    context.write(word, total_and_fd); //emit the current word and f(d)
                    }
                    i++;
                }

               
            }

        /** Returns the Func corresponding to FUNCNUM*/
        private Func funcFromNum(int funcNum) {
            Func func = null;
            switch (funcNum) {
                case 0:	
                    func = new Func() {
                        public double f(double d) {
                            return d == Double.POSITIVE_INFINITY ? 0.0 : 1.0;
                        }			
                    };	
                    break;
                case 1:
                    func = new Func() {
                        public double f(double d) {
                            return d == Double.POSITIVE_INFINITY ? 0.0 : 1.0 + 1.0 / d;
                        }			
                    };
                    break;
                case 2:
                    func = new Func() {
                        public double f(double d) {
                            return d == Double.POSITIVE_INFINITY ? 0.0 : 1.0 + Math.sqrt(d);
                        }			
                    };
                    break;
            }
            return func;
        }
    }

    /** Here's where you'll be implementing your combiner. It must be non-trivial for you to receive credit. */
    public static class Combine1 extends Reducer<Text, DoublePair, Text, DoublePair> {
            
    private DoublePair combiner_total_and_fd = new DoublePair(); // for combiner
    
        @Override 
            public void reduce(Text key, Iterable<DoublePair> values,
                    Context context) throws IOException, InterruptedException {

                 // YOUR CODE HERE
                double sum_fd = 0;
                double co_occurence = 0;
                double sum_occurence = 0;
                for (DoublePair value : values) {
                    sum_fd += value.getDouble2();
                    sum_occurence += value.getDouble1();
                }  
                combiner_total_and_fd.setDouble1(sum_occurence); //set the total occurences of current word to double1 in DoublePair
                combiner_total_and_fd.setDouble2(sum_fd); //set double2 to f(d)
                context.write(key, combiner_total_and_fd); 
            }
    }


    public static class Reduce1 extends Reducer<Text, DoublePair, DoubleWritable, Text> {
        @Override
            public void reduce(Text key, Iterable<DoublePair> values,
                    Context context) throws IOException, InterruptedException {

                // YOUR CODE HERE
                double sum_fd = 0;
                double co_occurence = 0;
                double sum_occurence = 0;
                for (DoublePair value : values) {
                    sum_fd += value.getDouble2();
                    sum_occurence += value.getDouble1();
                }
                if (sum_fd > 0){
                    co_occurence = -((sum_fd)*(Math.pow(Math.log(sum_fd), 3)))/sum_occurence;
                }    
                context.write(new DoubleWritable(co_occurence), key); 
            }
    }

    public static class Map2 extends Mapper<DoubleWritable, Text, DoubleWritable, Text> {
        //maybe do something, maybe don't
    }

    public static class Reduce2 extends Reducer<DoubleWritable, Text, DoubleWritable, Text> {

        int n = 0;
        static int N_TO_OUTPUT = 100;

        /*
         * Setup gets called exactly once for each reducer, before reduce() gets called the first time.
         * It's a good place to do configuration or setup that can be shared across many calls to reduce
         */
        @Override
            protected void setup(Context c) {
                n = 0;
            }

        /*
         * Your output should be a in the form of (DoubleWritable score, Text word)
         * where score is the co-occurrence value for the word. Your output should be
         * sorted from largest co-occurrence to smallest co-occurrence.
         */
        @Override
            public void reduce(DoubleWritable key, Iterable<Text> values,
                    Context context) throws IOException, InterruptedException {

                 // YOUR CODE HERE
                for (Text value : values){
                    if (n < N_TO_OUTPUT){
                        double key1 = Math.abs(key.get()) + 0;
                        
                        context.write(new DoubleWritable(key1), value); 
                    }
                    n++;
                }
            }
    }

    /*
     *  You shouldn't need to modify this function much. If you think you have a good reason to,
     *  you might want to discuss with staff.
     *
     *  The skeleton supports several options.
     *  if you set runJob2 to false, only the first job will run and output will be
     *  in TextFile format, instead of SequenceFile. This is intended as a debugging aid.
     *
     *  If you set combiner to false, the combiner will not run. This is also
     *  intended as a debugging aid. Turning on and off the combiner shouldn't alter
     *  your results. Since the framework doesn't make promises about when it'll
     *  invoke combiners, it's an error to assume anything about how many times
     *  values will be combined.
     */
    public static void main(String[] rawArgs) throws Exception {
        GenericOptionsParser parser = new GenericOptionsParser(rawArgs);
        Configuration conf = parser.getConfiguration();
        String[] args = parser.getRemainingArgs();

        boolean runJob2 = conf.getBoolean("runJob2", true);
        boolean combiner = conf.getBoolean("combiner", false);

        System.out.println("Target word: " + conf.get("targetWord"));
        System.out.println("Function num: " + conf.get("funcNum"));

        if(runJob2)
            System.out.println("running both jobs");
        else
            System.out.println("for debugging, only running job 1");

        if(combiner)
            System.out.println("using combiner");
        else
            System.out.println("NOT using combiner");

        Path inputPath = new Path(args[0]);
        Path middleOut = new Path(args[1]);
        Path finalOut = new Path(args[2]);
        FileSystem hdfs = middleOut.getFileSystem(conf);
        int reduceCount = conf.getInt("reduces", 32);

        if(hdfs.exists(middleOut)) {
            System.err.println("can't run: " + middleOut.toUri().toString() + " already exists");
            System.exit(1);
        }
        if(finalOut.getFileSystem(conf).exists(finalOut) ) {
            System.err.println("can't run: " + finalOut.toUri().toString() + " already exists");
            System.exit(1);
        }

        {
            Job firstJob = new Job(conf, "job1");

            firstJob.setJarByClass(Map1.class);

            /* You may need to change things here */
            firstJob.setMapOutputKeyClass(Text.class);
            firstJob.setMapOutputValueClass(DoublePair.class);
            firstJob.setOutputKeyClass(DoubleWritable.class);
            firstJob.setOutputValueClass(Text.class);
            /* End region where we expect you to perhaps need to change things. */

            firstJob.setMapperClass(Map1.class);
            firstJob.setReducerClass(Reduce1.class);
            firstJob.setNumReduceTasks(reduceCount);


            if(combiner)
                firstJob.setCombinerClass(Combine1.class);

            firstJob.setInputFormatClass(SequenceFileInputFormat.class);
            if(runJob2)
                firstJob.setOutputFormatClass(SequenceFileOutputFormat.class);

            FileInputFormat.addInputPath(firstJob, inputPath);
            FileOutputFormat.setOutputPath(firstJob, middleOut);

            firstJob.waitForCompletion(true);
        }

        if(runJob2) {
            Job secondJob = new Job(conf, "job2");

            secondJob.setJarByClass(Map1.class);
            /* You may need to change things here */
            secondJob.setMapOutputKeyClass(DoubleWritable.class);
            secondJob.setMapOutputValueClass(Text.class);
            secondJob.setOutputKeyClass(DoubleWritable.class);
            secondJob.setOutputValueClass(Text.class);
            /* End region where we expect you to perhaps need to change things. */

            secondJob.setMapperClass(Map2.class);
            secondJob.setReducerClass(Reduce2.class);

            secondJob.setInputFormatClass(SequenceFileInputFormat.class);
            secondJob.setOutputFormatClass(TextOutputFormat.class);
            secondJob.setNumReduceTasks(1);


            FileInputFormat.addInputPath(secondJob, middleOut);
            FileOutputFormat.setOutputPath(secondJob, finalOut);

            secondJob.waitForCompletion(true);
        }
    }

}
