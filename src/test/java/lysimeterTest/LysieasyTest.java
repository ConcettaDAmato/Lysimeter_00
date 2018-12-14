package lysimeterTest;

import java.net.URISyntaxException;
import java.util.*;
import org.jgrasstools.gears.io.timedependent.OmsTimeSeriesIteratorReader;
import org.jgrasstools.gears.io.timedependent.OmsTimeSeriesIteratorWriter;

import lysimeter.*;
import bufferWriter.BufferLysimeterFirst;
import monodimensionalProblemTimeDependent.*;


import org.junit.Test;

/**
* Test the {@link TestRichards1DSolver} module.
* 
* @author Niccolo' Tubini, Michele Bottazzi, Concetta D'Amato, Francesco Serafin
*/
public class LysieasyTest {

@Test
public void Test() throws Exception {

	String startDate = "2018-03-05 00:00" ;
	String endDate   = "2018-03-05 00:05";
	int timeStepMinutes = 5;
	String fId = "ID";
	String pathTopBC    = "resources/Input/TestAll_0.csv";
	String pathBottomBC = "resources/Input/TestAll_0.csv";
	String pathET       = "resources/Input/Transpiration.csv";
	String pathGrid     = "resources/Input/Sand_VG_03.nc";
	String outPathET    = "resources/Output/StressET.csv";
		
	OmsTimeSeriesIteratorReader topBCReader = getTimeseriesReader(pathTopBC, fId, startDate, endDate, timeStepMinutes);
	OmsTimeSeriesIteratorReader bottomBCReader = getTimeseriesReader(pathBottomBC, fId, startDate, endDate, timeStepMinutes);
	OmsTimeSeriesIteratorReader etReader = getTimeseriesReader(pathET, fId, startDate, endDate, timeStepMinutes);

	OmsTimeSeriesIteratorWriter etWriter = new OmsTimeSeriesIteratorWriter();
	etWriter.file = outPathET;
	etWriter.tStart = startDate;
	etWriter.tTimestep = timeStepMinutes;
	etWriter.fileNovalue="-9999";
		
	BufferLysimeterFirst buffer = new BufferLysimeterFirst();
	WriteNetCDFLysimeterFirst writeNetCDF = new WriteNetCDFLysimeterFirst();
	ReadNetCDFLysimeterFirst readNetCDF = new ReadNetCDFLysimeterFirst();
		
	LysimeterFirst lisy = new LysimeterFirst();
		
	readNetCDF.richardsGridFilename = pathGrid;
	readNetCDF.read();
				
	lisy.z = readNetCDF.z;
	lisy.spaceDeltaZ = readNetCDF.spaceDelta;
	lisy.psiIC = readNetCDF.psiIC;
	lisy.deltaZ = readNetCDF.deltaZ;
	lisy.ks = readNetCDF.Ks;
	lisy.thetaS = readNetCDF.thetaS;
	lisy.thetaR = readNetCDF.thetaR;
	lisy.par1SWRC = readNetCDF.par1SWRC;
	lisy.par2SWRC = readNetCDF.par2SWRC;
	lisy.par3SWRC = readNetCDF.par3SWRC;
	lisy.par4SWRC = readNetCDF.par4SWRC;
	lisy.par5SWRC = readNetCDF.par5SWRC;
	lisy.psiStar1 = readNetCDF.par6SWRC;
	lisy.psiStar2 = readNetCDF.par7SWRC;
	lisy.psiStar3 = readNetCDF.par8SWRC;
	lisy.alphaSpecificStorage = readNetCDF.alphaSS;
	lisy.betaSpecificStorage = readNetCDF.betaSS;
	lisy.et = readNetCDF.et;
	lisy.soilHydraulicModel = "van genuchten";
	lisy.interfaceHydraulicCondType = "mean";
	lisy.topBCType = "Top Neumann";
	lisy.bottomBCType = "Bottom Free Drainage";
	lisy.delta = 0;
	lisy.tTimestep = 300;
	lisy.timeDelta = 300;
	lisy.newtonTolerance = Math.pow(10,-11);
	lisy.dir = "resources/Output";
	lisy.nestedNewton =1;
	lisy.thetaWp = 0.1;
	lisy.thetaFc = 0.387;
	lisy.rootsDepth = 1;
		
	//R1DSolver.picardIteration = 1;
	while( topBCReader.doProcess  ) {
		topBCReader.nextRecord();	
		HashMap<Integer, double[]> bCValueMap = topBCReader.outData;
		lisy.inTopBC= bCValueMap;
		etReader.nextRecord();
		bCValueMap = etReader.outData;
		lisy.inTranspiration = bCValueMap;
		bottomBCReader.nextRecord();
		bCValueMap = bottomBCReader.outData;
		lisy.inBottomBC = bCValueMap;
		lisy.inCurrentDate = topBCReader.tCurrent;	
		lisy.solve();
		buffer.inputDate = lisy.inCurrentDate;
		buffer.inputSpatialCoordinate = readNetCDF.eta;
		buffer.inputDualSpatialCoordinate = readNetCDF.etaDual;
		buffer.inputVariable = lisy.outputToBuffer;
		buffer.solve();
			
		writeNetCDF.fileName = "resources/Output/Out_Eclipse_SandVG_02_30.nc";
		writeNetCDF.briefDescritpion = "\n	Test problem 1 layers of clay no storativity, con il codice vecchio stile\n	"
				+ "Initial condition hydrostatic no ponding\n		"
				+ "BC: top 2mm rainfall each 5min, bottom Dirichlet\n		"
				//+ "Clay parameters BC: ks=0.000023m/s, psiD= -34m, n=1.5, thetaR=0.07, thetaS=0.35, alphaStorativity= 0 1/Pa, betaStorativity=0 1/Pa \n		"
				+ "Sand parameters: ks=0.003697m/s, alpha= 1.47m-1, n=1.7, thetaR=0.02, thetaS=0.38, alphaStorativity= 0 1/Pa, betaStorativity= 0 1/Pa\n		"
				+ "Grid input file: " + pathGrid +"\n		"
				+ "TopBC input file: " + pathTopBC +"\n		"
				+ "BottomBC input file: " + pathBottomBC +"\n		"
				+ "DeltaT: 300s\n		"
				+ "Picard iteration: 1\n		"
				   + "Interface k: mean";
		writeNetCDF.myVariables = buffer.myVariable;
		writeNetCDF.mySpatialCoordinate = buffer.mySpatialCoordinate;
		writeNetCDF.myDualSpatialCoordinate = buffer.myDualSpatialCoordinate;			
		writeNetCDF.doProcess = topBCReader.doProcess;
		writeNetCDF.writeNetCDF(); 	
	}
		
	topBCReader.close();
	bottomBCReader.close();
	etReader.close();
}

private OmsTimeSeriesIteratorReader getTimeseriesReader( String inPath, String id, String startDate, String endDate,
		int timeStepMinutes ) throws URISyntaxException {
	OmsTimeSeriesIteratorReader reader = new OmsTimeSeriesIteratorReader();
	reader.file = inPath;
	reader.idfield = "ID";
	reader.tStart = startDate;
	reader.tTimestep = timeStepMinutes;
	reader.tEnd = endDate;
	reader.fileNovalue = "-9999";
	reader.initProcess();
	return reader;
}
}
