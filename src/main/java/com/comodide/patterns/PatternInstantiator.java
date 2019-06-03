package com.comodide.patterns;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.protege.editor.owl.model.OWLModelManager;
import org.protege.editor.owl.model.entity.EntityCreationPreferences;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotationAssertionAxiom;
import org.semanticweb.owlapi.model.OWLAnnotationProperty;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassAssertionAxiom;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyChange;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.util.OWLEntityRenamer;

// TODO: Documentation for the whole class
public class PatternInstantiator {
	
	private OWLOntology pattern;
	private final Boolean useTargetNamespace;
	private final String patternLabel;
	private final IRI createdModuleIri;
	private final IRI targetOntologyIri;
	private final String entitySeparator;
	
	private static final String OPLA_NAMESPACE = "http://ontologydesignpatterns.org/opla#";
	
	public PatternInstantiator(OWLOntology pattern, String patternLabel, OWLModelManager modelManager) {
		super();
		this.pattern = pattern;
		this.patternLabel = patternLabel;
		this.useTargetNamespace = PatternInstantiationConfiguration.getUseTargetNamespace();
		this.targetOntologyIri = modelManager.getActiveOntology().getOntologyID().getOntologyIRI().or(IRI.generateDocumentIRI());
		this.entitySeparator = EntityCreationPreferences.getDefaultSeparator();
		String moduleName = String.format("-modules/%s", UUID.randomUUID().toString());
		this.createdModuleIri = IRI.create(targetOntologyIri.toString(), moduleName);
	}

	public Set<OWLAxiom> getInstantiationAxioms() {
		if (useTargetNamespace) {
			OWLOntologyManager patternManager = pattern.getOWLOntologyManager();
			OWLEntityRenamer renamer = new OWLEntityRenamer(patternManager, Collections.singleton(pattern));
			for (OWLEntity entity: pattern.getSignature()) {
				if (!entity.isBuiltIn() && !entity.getIRI().toString().contains(OPLA_NAMESPACE)) {
					String entityShortName = this.entitySeparator + entity.getIRI().getShortForm();
					IRI newIRI = IRI.create(targetOntologyIri.toString(), entityShortName);
					List<OWLOntologyChange> changes = renamer.changeIRI(entity, newIRI);
					patternManager.applyChanges(changes);
				}
			}
		}
		return pattern.getAxioms();
	}
	
	public Set<OWLAxiom> getModuleAnnotationAxioms() {
		// 0. Create temporary manager, factory, and return value holder
		OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
		OWLDataFactory factory = manager.getOWLDataFactory();
		Set<OWLAxiom> moduleAnnotationAxioms = new HashSet<OWLAxiom>();
		
		// 1. Get pattern IRI
		IRI patternIRI = pattern.getOntologyID().getOntologyIRI().or(IRI.generateDocumentIRI());
		
		// 2. Define that <patternIRI> rdf:type opla:pattern; rdfs:label PatternLabel
		OWLClass oplaPatternClass = factory.getOWLClass(IRI.create(String.format("%sPattern", OPLA_NAMESPACE)));
		OWLNamedIndividual patternIndividual = factory.getOWLNamedIndividual(patternIRI);
		OWLClassAssertionAxiom patternTypingAxiom = factory.getOWLClassAssertionAxiom(oplaPatternClass, patternIndividual);
		moduleAnnotationAxioms.add(patternTypingAxiom);
		OWLAnnotationAssertionAxiom patternLabelAxiom = factory.getOWLAnnotationAssertionAxiom(factory.getRDFSLabel(), patternIRI, factory.getOWLLiteral(patternLabel));
		moduleAnnotationAxioms.add(patternLabelAxiom);
		
		// 2.5 define that <moduleIri>  rdf:type opla:Module; rdfs:label SomethingReasonable
		OWLClass oplaModuleClass = factory.getOWLClass(IRI.create(String.format("%sModule", OPLA_NAMESPACE)));
		OWLNamedIndividual moduleIndividual = factory.getOWLNamedIndividual(createdModuleIri);
		OWLClassAssertionAxiom moduleTypingAxiom = factory.getOWLClassAssertionAxiom(oplaModuleClass, moduleIndividual);
		moduleAnnotationAxioms.add(moduleTypingAxiom);
		OWLAnnotationAssertionAxiom moduleLabelAxiom = factory.getOWLAnnotationAssertionAxiom(factory.getRDFSLabel(), createdModuleIri, factory.getOWLLiteral(String.format("'%s' ODP Instantiation Module", patternLabel)));
		moduleAnnotationAxioms.add(moduleLabelAxiom);
		
		// 2.6 define that the module reusesPatternAsTemplate thePattern
		OWLAnnotationProperty reusesPatternAsTemplate = factory.getOWLAnnotationProperty(IRI.create(String.format("%sreusesPatternAsTemplate", OPLA_NAMESPACE)));
		OWLAnnotationAssertionAxiom patternReuseAxiom = factory.getOWLAnnotationAssertionAxiom(reusesPatternAsTemplate, createdModuleIri, patternIRI);
		moduleAnnotationAxioms.add(patternReuseAxiom);
		
		// 3. For each entity in instantiated pattern module: annotate that it opla:isNativeTo <moduleIRI>
		OWLAnnotationProperty isNativeTo = factory.getOWLAnnotationProperty(IRI.create(String.format("%sisNativeTo", OPLA_NAMESPACE)));
		for (OWLEntity entity: pattern.getSignature()) {
			if (!entity.isBuiltIn() && !entity.getIRI().toString().contains(OPLA_NAMESPACE)) {
				OWLAnnotationAssertionAxiom entityNativeToAxiom = factory.getOWLAnnotationAssertionAxiom(isNativeTo, entity.getIRI(), createdModuleIri);
				moduleAnnotationAxioms.add(entityNativeToAxiom);
			}
		}
		
		return moduleAnnotationAxioms;
	}

}