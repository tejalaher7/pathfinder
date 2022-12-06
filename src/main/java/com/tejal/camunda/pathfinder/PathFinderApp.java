package com.tejal.camunda.pathfinder;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Scanner;

import org.camunda.bpm.model.bpmn.Bpmn;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.FlowNode;
import org.camunda.bpm.model.bpmn.instance.SequenceFlow;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;


public class PathFinderApp 
{
	private static LinkedList<String> result;
	
	public PathFinderApp()
	{
		result= new LinkedList<String>();
	}
	
    public static void main( String[] args )
    {
       //Storing the command line arguments
       String startNodeId = args[0];
       String endNodeId = args[1];
         
       PathFinderApp pathfinder = new PathFinderApp();
       
       
       //to get the BPMN Model
       BpmnModelInstance modelInstance =pathfinder.extractBPMNModel(startNodeId,endNodeId) ;
       
       //to find path
       pathfinder.printPath(modelInstance,startNodeId,endNodeId);  
                     
    }
    
    
    private BpmnModelInstance extractBPMNModel(String startNodeId,String endNodeId)
    {

        String inline = "";
        BpmnModelInstance modelInstance = null;
    	try {
    		//Hitting remote server to get the BPMN model
            URL url = new URL("https://n35ro2ic4d.execute-api.eu-central-1.amazonaws.com/prod/engine-rest/process-definition/key/invoice/xml");

            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.connect();
            int responsecode = conn.getResponseCode();

            if (responsecode != 200) {
                throw new RuntimeException("HttpResponseCode: " + responsecode);
            } 
            
            else {

                Scanner scanner = new Scanner(url.openStream());
                //Write all the JSON data into a string using a scanner
                while (scanner.hasNext()) {
                    inline += scanner.nextLine();
                }
                scanner.close();
                
              //Using the JSON simple library to parse the string response into a json object
                JSONParser parser = new JSONParser();
                JSONObject data_obj;
				
				data_obj = (JSONObject) parser.parse(inline);
				
                InputStream stream = new ByteArrayInputStream( data_obj.get("bpmn20Xml").toString().getBytes());
                modelInstance = Bpmn.readModelFromStream(stream);
            }
    	}
            catch (Exception e) 
    		{
            e.printStackTrace();
    		}
    	
    	return modelInstance;
    	
    }
	
    //Method to print a Path between source and target
    private void printPath(BpmnModelInstance modelInstance, String startNodeId, String endNodeId) {
    	
    	//Extracting the flowNodes from BPMN instance 
        FlowNode source = modelInstance.getModelElementById(startNodeId);
        FlowNode target = modelInstance.getModelElementById(endNodeId);
        
        if(source.equals(target))
        {
        	System.out.println("The source and destination are the same");
        	System.exit(-1);
        }
        
        //LinkedList to save the nodes of the path
        LinkedList<String> path = new LinkedList<String>();
        
        //ArrayList for noting the visited nodes to avoid cycles
        ArrayList<String> isVisited = new ArrayList<String>(); 
        path.add(source.getId());
        
         // Call recursive utility
        
        findPathUtil(source, target, isVisited,path);
        if(result.size()!=0)
        {
        	System.out.println("The path from "+source.getId()+" to "+target.getId()+" is:");
        	System.out.println(result);
        }
        else
        {
        	System.out.println("Path not found");
        	System.exit(-1);
        }
       }

	
	//recursive function to implement the logic of findPath

	private  void findPathUtil(FlowNode source, FlowNode target, ArrayList<String> isVisited, LinkedList<String> path) {
		
		// List for saving adjacent FlowNodes
		ArrayList<FlowNode> followingFlowNodes = new ArrayList<FlowNode>();
		
		//Iteration for extracting the adjacent nodes to a current node
 	   for (SequenceFlow sequenceFlow : source.getOutgoing()) 
 	   {
            followingFlowNodes.add(sequenceFlow.getTarget());
            
 	   }
 	   
 	  if (source.equals(target)) {
			//checking size against 1 as the source node is already added to the path
			result.addAll(path);
			return;
 	  }

 	  else{
		
    	   isVisited.add(source.getId());
    	   //Depth first logic to extract path from source to target
    	   for(FlowNode flownode : followingFlowNodes)
    	   {
    		   
    		   if(!(isVisited.contains(flownode.getId())))
    		{	
    			   path.add(flownode.getId());
    			   findPathUtil(flownode,target,isVisited,path);
    			   path.remove(flownode.getId());
    			   
    		}
    		   
    	   }
    	  isVisited.remove(source.getId());
		
		   }
	}
       
}

