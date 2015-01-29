/*******************************************************************************
 * The MIT License (MIT)
 *
 * Copyright (c) 2011, 2013 OpenWorm.
 * http://openworm.org
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the MIT License
 * which accompanies this distribution, and is available at
 * http://opensource.org/licenses/MIT
 *
 * Contributors:
 *     	OpenWorm - http://openworm.org/people.html
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
 * OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE
 * USE OR OTHER DEALINGS IN THE SOFTWARE.
 *******************************************************************************/
package org.geppetto.model.neuroml.services;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import org.apache.tools.ant.util.FileUtils;
import org.geppetto.core.beans.ModelInterpreterConfig;
import org.geppetto.core.model.AModelInterpreter;
import org.geppetto.core.model.IModel;
import org.geppetto.core.model.IModelInterpreter;
import org.geppetto.core.model.ModelInterpreterException;
import org.geppetto.core.model.ModelWrapper;
import org.geppetto.core.model.runtime.AspectNode;
import org.geppetto.core.model.runtime.AspectSubTreeNode;
import org.geppetto.core.model.runtime.CompositeNode;
import org.geppetto.core.model.runtime.EntityNode;
import org.geppetto.core.model.runtime.AspectSubTreeNode.AspectTreeType;
import org.geppetto.core.utilities.URLReader;
import org.geppetto.model.neuroml.utils.LEMSAccessUtility;
import org.geppetto.model.neuroml.utils.NeuroMLAccessUtility;
import org.geppetto.model.neuroml.utils.OptimizedLEMSReader;
import org.lemsml.jlems.api.LEMSDocumentReader;
import org.lemsml.jlems.api.interfaces.ILEMSDocument;
import org.lemsml.jlems.api.interfaces.ILEMSDocumentReader;
import org.lemsml.jlems.core.sim.ContentError;
import org.lemsml.jlems.core.type.Lems;
import org.neuroml.model.Base;
import org.neuroml.model.BaseCell;
import org.neuroml.model.NeuroMLDocument;
import org.neuroml.model.util.NeuroMLException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import parser.LemsParser;
import parser.LemsXmlUtils;



/**
 * @author matteocantarelli
 * 
 */
@Service
public class LEMSModelInterpreterService extends AModelInterpreter
{

	private NeuroMLModelInterpreterService _neuroMLModelInterpreter = new NeuroMLModelInterpreterService();
	
	@Autowired
	private ModelInterpreterConfig jlemsModelInterpreterConfig;

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.openworm.simulationengine.core.model.IModelProvider#readModel(java .lang.String)
	 */
	public IModel readModel(URL url, List<URL> recordings, String instancePath) throws ModelInterpreterException
	{
		ModelWrapper model = new ModelWrapper(instancePath);
		try
		{
			OptimizedLEMSReader reader = new OptimizedLEMSReader();
//			String lemsString = reader.read(url);
//
//			ILEMSDocumentReader lemsReader = new LEMSDocumentReader();
//			ILEMSDocument document = lemsReader.readModel(lemsString);
			
			
			
			File schema = new File(getClass().getResource("/Schemas/LEMS_v0.9.0.xsd").getFile());
			LemsParser parser = new LemsParser(url, schema);
			parser.processIncludes();
			parser.populateNameComponentTypeHM();
			parser.decorateComponentsWithType();
			
			model = new ModelWrapper(UUID.randomUUID().toString());
			model.setInstancePath(instancePath);
			// two different representation of the same file, one used to
			// simulate the other used to visualize
			if(reader.getNeuroMLs().size() == 1)
			{
				model.wrapModel(NeuroMLAccessUtility.NEUROML_ID, reader.getNeuroMLs().values().toArray()[0]);
			}
			else
			{
				model.wrapModel(NeuroMLAccessUtility.NEUROML_ID, reader.getNeuroMLs());
			}
			//TODO: This need to be changed (BaseCell, String)
			model.wrapModel(NeuroMLAccessUtility.SUBENTITIES_MAPPING_ID, new HashMap<BaseCell, EntityNode>());
			model.wrapModel(NeuroMLAccessUtility.CELL_SUBENTITIES_MAPPING_ID, new HashMap<String, BaseCell>());
			model.wrapModel(NeuroMLAccessUtility.LEMS_ID, parser.getLems());
			model.wrapModel(NeuroMLAccessUtility.URL_ID, url);
			model.wrapModel(NeuroMLAccessUtility.DISCOVERED_COMPONENTS, new HashMap<String, Base>());
			model.wrapModel(LEMSAccessUtility.DISCOVERED_LEMS_COMPONENTS, new HashMap<String, Object>());
			
			model.wrapModel(NeuroMLAccessUtility.DISCOVERED_NESTED_COMPONENTS_ID, new ArrayList<String>());
			
			addRecordings(recordings, instancePath, model);
		}
		catch(Exception e)
		{
			System.out.println("Failed!");
			e.printStackTrace();
		} catch (Throwable e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return model;
	}



	/*
	 * (non-Javadoc)
	 * 
	 * @see org.geppetto.core.model.IModelInterpreter#populateModelTree(org.geppetto.core.model.runtime.AspectNode)
	 */
	@Override
	public boolean populateModelTree(AspectNode aspectNode) throws ModelInterpreterException
	{
//		return _neuroMLModelInterpreter.populateModelTree(aspectNode);
		
		boolean modified = false;

		AspectSubTreeNode modelTree = (AspectSubTreeNode) aspectNode.getSubTree(AspectTreeType.MODEL_TREE);
		modelTree.setId(AspectTreeType.MODEL_TREE.toString());

		IModel model = aspectNode.getModel();
		try
		{
			extended.Lems lems = (extended.Lems) ((ModelWrapper) model).getModel(NeuroMLAccessUtility.LEMS_ID);
			if(lems != null)
			{
				//modified = populateModelTree.populateModelTree(modelTree, ((ModelWrapper) model));
				modelTree.addChild(new CompositeNode("id"));
				modelTree.setModified(true);
			}

		}
		catch(Exception e)
		{
			throw new ModelInterpreterException(e);
		}
		return modified;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.geppetto.core.model.IModelInterpreter#populateRuntimeTree(org.geppetto.core.model.runtime.AspectNode)
	 */
	@Override
	public boolean populateRuntimeTree(AspectNode aspectNode) throws ModelInterpreterException
	{
		return _neuroMLModelInterpreter.populateRuntimeTree(aspectNode);
	}

	@Override
	public String getName()
	{
		return this.jlemsModelInterpreterConfig.getModelInterpreterName();
	}

}
