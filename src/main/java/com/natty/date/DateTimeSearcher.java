package com.natty.date;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.antlr.runtime.ANTLRInputStream;
import org.antlr.runtime.CommonTokenStream;
import org.antlr.runtime.RecognitionException;
import org.antlr.runtime.debug.BlankDebugEventListener;
import org.antlr.runtime.tree.CommonTreeNodeStream;
import org.antlr.runtime.tree.Tree;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.natty.date.generated.DateLexer;
import com.natty.date.generated.DateParser;
import com.natty.date.generated.DateWalker;


public class DateTimeSearcher {
  private static final Logger _logger = Logger.getLogger(DateTimeSearcher.class.getName());
  private static final DateFormat _formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
  
  public static String search(String inputString) {
    inputString = inputString.trim() + " foo";
    ANTLRInputStream input = null;
    ParseEventListener parseListener = new ParseEventListener();
    DateWalker walker = null;
    try {
      // lex
      input = new ANTLRNoCaseInputStream(new ByteArrayInputStream(inputString.getBytes()));
      DateLexer lexer = new DateLexer(input);
      CommonTokenStream tokens = new CommonTokenStream(lexer);
      
      // parse 
      DateParser parser = new DateParser(tokens, parseListener);
      DateParser.search_return result = parser.search();
      
      // walk
      Tree tree = (Tree) result.getTree();
      System.out.println(tree.toStringTree());
      CommonTreeNodeStream nodes = new CommonTreeNodeStream(tree);
      nodes.setTokenStream(tokens);
      walker = new DateWalker(nodes, new BlankDebugEventListener());
      walker.search();
      
    } catch (IOException e) {
      e.printStackTrace();
      
    } catch (RecognitionException e) {
      e.printStackTrace();
    }
    
    List<Location> locations = parseListener.getLocations();
    List<Date> dateTimes = walker.getState().getDateTimes();
    
    JSONArray jsonArray = new JSONArray();
    for(int i=0; i<locations.size(); i++) {
      Location location = locations.get(i);
      Date date = dateTimes.get(i);
      try {
        JSONObject json = new JSONObject();
        json.put("text", location.getText());
        json.put("start", location.getStart());
        json.put("end", location.getEnd());
        _formatter.setTimeZone(TimeZone.getTimeZone("America/New_York"));
        json.put("localtime", _formatter.format(date));
        _formatter.setTimeZone(TimeZone.getTimeZone("UTC"));
        json.put("utctime", _formatter.format(date));
        jsonArray.put(json);
      
      } catch(JSONException e) {
        _logger.log(Level.FINE, "could not generate json", e);
      
      }
    }
    
    return jsonArray.toString();
  }
  
  /**
   * Parses the input string for a single date time and assumes there is no
   * extraneous text.
   * @param inputString
   * @return
   */
  public static String parseDate(final String inputString) {
    ANTLRInputStream input = null;
    StructureBuilder builder = new StructureBuilder();
    Date date = new Date();
    Tree tree = null;
    try {
      // lex
      input = new ANTLRNoCaseInputStream(new ByteArrayInputStream(inputString.getBytes()));
      DateLexer lexer = new DateLexer(input);
      CommonTokenStream tokens = new CommonTokenStream(lexer);
      
      // parse 
      DateParser parser = new DateParser(tokens, builder);
      DateParser.date_time_return result = parser.date_time();
      
      // walk
      tree = (Tree) result.getTree();
      CommonTreeNodeStream nodes = new CommonTreeNodeStream(tree);
      nodes.setTokenStream(tokens);
      DateWalker walker = new DateWalker(nodes, new BlankDebugEventListener());
      walker.date_time();
      date = walker.getState().getDateTimes().get(0);
      
    } catch (IOException e) {
      e.printStackTrace();
      
    } catch (RecognitionException e) {
      e.printStackTrace();
    }
    
    JSONObject json = new JSONObject();
    try {
      DateFormat friendlyFormatter = new SimpleDateFormat("EEE MMM dd, yyyy hh:mm a z");
      json.put("iso8601", friendlyFormatter.format(date));
      _formatter.setTimeZone(TimeZone.getTimeZone("America/New_York"));
      json.put("localtime", _formatter.format(date));
      _formatter.setTimeZone(TimeZone.getTimeZone("UTC"));
      json.put("utctime", _formatter.format(date));
      json.put("structure", builder.toJSON());
      if(tree != null) json.put("ast", tree.toStringTree());
      
    } catch(JSONException e) {
      _logger.log(Level.FINE, "could not generate json", e);
      
    }
    
    return json.toString();
  }
}