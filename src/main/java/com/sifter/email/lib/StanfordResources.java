package com.sifter.email.lib;
import com.sifter.email.model.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Properties;
import java.io.StringReader;

import edu.stanford.nlp.process.TokenizerFactory;
import edu.stanford.nlp.process.CoreLabelTokenFactory;
import edu.stanford.nlp.process.DocumentPreprocessor;
import edu.stanford.nlp.process.PTBTokenizer;
import edu.stanford.nlp.ling.CoreAnnotations.NamedEntityTagAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.PartOfSpeechAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TextAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.HasWord;
import edu.stanford.nlp.ling.Sentence;
import edu.stanford.nlp.semgraph.SemanticGraph;
import edu.stanford.nlp.semgraph.SemanticGraphCoreAnnotations.CollapsedCCProcessedDependenciesAnnotation;
import edu.stanford.nlp.trees.*;
import edu.stanford.nlp.trees.TreeCoreAnnotations.TreeAnnotation;
import edu.stanford.nlp.util.CoreMap;
import edu.stanford.nlp.models.lexparser.*;
import edu.stanford.nlp.parser.lexparser.LexicalizedParser;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import gate.util.Out;

/*
 * To use the Stanford Parser for every body that is received
 * @author : Kiran K.
 */
public class StanfordResources {



	private StanfordCoreNLP pipeline;
	private LexicalizedParser lp = null;
	private String tp;

	// This option shows loading and sentence-segmenting and tokenizing
	// a file using DocumentPreprocessor.
	private TreebankLanguagePack tlp;
	private GrammaticalStructureFactory gsf;

	public StanfordResources()
	{
		Properties props = new Properties();
		props.put("annotators", "tokenize, ssplit, pos, lemma, ner, parse");
		pipeline = new StanfordCoreNLP(props);
		lp = LexicalizedParser.loadModel("edu/stanford/nlp/models/lexparser/englishPCFG.ser.gz");
		tlp = new PennTreebankLanguagePack();
		gsf  = tlp.grammaticalStructureFactory();
	}

	public void setThreadPart(String threadPart)
	{
		tp = threadPart;
		Out.prln(threadPart);
	}

	public String getThreadPart()
	{
		return tp;
	}


	public ArrayList<String> getPhrases(String text, String pos){
		ArrayList<String> phrases = new ArrayList<String>();
		TokenizerFactory<CoreLabel> tokenizerFactory = PTBTokenizer.factory(new CoreLabelTokenFactory(), "");
		List<CoreLabel> rawWords = tokenizerFactory.getTokenizer(new StringReader(text)).tokenize();
		Tree currTree = lp.apply(rawWords);

		for(Tree sentence: getSentenceTrees(text)){
			ArrayList<Tree> treeList = new ArrayList<Tree>();
			getPhrases(treeList, sentence,pos);

			for(Tree t : treeList){
				TreebankLanguagePack tlp = new PennTreebankLanguagePack();
				GrammaticalStructureFactory gsf = tlp.grammaticalStructureFactory();
				GrammaticalStructure gs = gsf.newGrammaticalStructure(t);
				StringBuilder sb = new StringBuilder();
				boolean hasStarted = false;
				List<TreeGraphNode> graphNodes = new ArrayList<TreeGraphNode>(gs.getNodes());
				Collections.sort(graphNodes, new TreeGraphComparator());
				for(TreeGraphNode tgn: graphNodes){
					//System.out.println(tgn.label().word());
					String word = tgn.label().word();

					if(word != null){
						if(word.matches("[\\dA-Za-z ]*[:]?[\\dA-Za-z& ]+") && hasStarted){
							sb.append(" ");
						}
						hasStarted = true;
						sb.append(word);

					}
				}
				phrases.add(sb.toString());
				Out.prln(sb);
				Out.prln();
			}
		}



		return phrases;
	}




	private void getPhrases(ArrayList<Tree> treeList, Tree currTree, String pos){
		if(currTree == null){
			return;
		}
		else if(currTree.label().toString().equals(pos)){
			treeList.add(currTree);
		}

		else{
			for(Tree t:currTree.getChildrenAsList()){
				getPhrases(treeList,t,pos);
			}
		}
	}


	private ArrayList<Tree> getSentenceTrees(String text){
		// creates a StanfordCoreNLP object, with POS tagging, lemmatization, NER, parsing, and coreference resolution 


		// read some text in the text variable

		// create an empty Annotation just with the given text
		Annotation document = new Annotation(text);

		// run all Annotators on this text
		pipeline.annotate(document);

		// these are all the sentences in this document
		// a CoreMap is essentially a Map that uses class objects as keys and has values with custom types
		List<CoreMap> sentences = document.get(SentencesAnnotation.class);
		ArrayList<Tree> trees = new ArrayList<Tree>();
		for(CoreMap sentence: sentences) {
			// traversing the words in the current sentence
			// a CoreLabel is a CoreMap with additional token-specific methods
			for (CoreLabel token: sentence.get(TokensAnnotation.class)) {
				// this is the text of the token
				String word = token.get(TextAnnotation.class);
				// this is the POS tag of the token
				String pos = token.get(PartOfSpeechAnnotation.class);
				// this is the NER label of the token
				String ne = token.get(NamedEntityTagAnnotation.class); 
				System.out.println(word );
			}

			// this is the parse tree of the current sentence
			Tree tree = sentence.get(TreeAnnotation.class);
			trees.add(tree);
			// this is the Stanford dependency graph of the current sentence
			//SemanticGraph dependencies = sentence.get(CollapsedCCProcessedDependenciesAnnotation.class);
		}
		return trees;

	}



	public void parseThreadPart()
	{
		try
		{
			String parseSentence = getThreadPart();

			// This option shows loading and using an explicit tokenizer
			TokenizerFactory<CoreLabel> tokenizerFactory = PTBTokenizer.factory(new CoreLabelTokenFactory(), "");
			List<CoreLabel> rawWords2 = tokenizerFactory.getTokenizer(new StringReader(parseSentence)).tokenize();
			Tree parse = lp.apply(rawWords2);
			//parse.
			TreebankLanguagePack tlp = new PennTreebankLanguagePack();
			GrammaticalStructureFactory gsf = tlp.grammaticalStructureFactory();
			GrammaticalStructure gs = gsf.newGrammaticalStructure(parse);
			StringBuilder sb = new StringBuilder();
			//		    for(TreeGraphNode t: gs.getNodes()){
			//		    	System.out.println(t.label().word());
			//		    }
			//List<TypedDependency> tdl = gs.typedDependenciesCCprocessed();
			//		    System.out.println();
			//		    System.out.println(tdl);
			//		    System.out.println();

			Out.prln("******************NP*****************************");
			getPhrases(parseSentence,"NP");
			Out.prln("******************/NP*****************************");
			Out.prln();

			Out.prln("******************VP*****************************");
			getPhrases(parseSentence,"VP");
			Out.prln("******************/VP*****************************");
			TreePrint tp = new TreePrint("penn,typedDependenciesCollapsed");
			//tp.printTree(parse);

			//tp.

		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}


	class TreeGraphComparator implements Comparator<TreeGraphNode> {

		@Override
		public int compare(TreeGraphNode arg0, TreeGraphNode arg1) {

			return arg0.index() - arg1.index();
		}
	}

}

