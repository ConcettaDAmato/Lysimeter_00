/*
 * GNU GPL v3 License
 *
 * Copyright 2016 Marialaura Bancheri
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

//MODELLO ORIGINALE MICHELE
package lysimeter;

import java.util.ArrayList;import java.util.HashMap;
import java.util.LinkedHashMap;

import oms3.annotations.*;

import org.apache.commons.math3.analysis.interpolation.LinearInterpolator;
import org.apache.commons.math3.analysis.polynomials.PolynomialSplineFunction;
import org.geotools.feature.SchemaException;

import richards_classes.*;
import monodimensionalProblemTimeDependent.WriteNetCDFRichardsParameterization;

@SuppressWarnings("unused")
@Description("Solve the Richards equation for the 1D domain.")
@Documentation("")
@Author(name = "Aaron Iemma, Niccolo' Tubini, Michele Bottazzi, Concetta D'Amato, Francesco Serafin, Michael Dumbser and Riccardo Rigon", contact = "tubini.niccolo@gmail.com")
@Keywords("Hydrology, Richards, Infiltration")
@Bibliography("Casulli (2010)")
//@Label(JGTConstants.HYDROGEOMORPHOLOGY)
//@Name("shortradbal")
//@Status(Status.CERTIFIED)
@License("General Public License Version 3 (GPLv3)")
public class Lysieasy {
	
	@Description("The ET.")
	@In
	@Unit("mm/min")
	public HashMap<Integer, double[]> inTranspiration;
	
	public double[] outET;
	
	@Description("The ET.")
	@Out
	@Unit("mm/min")
	public HashMap<Integer, double[]> outStressedTranspiration;
	
	@Description("Theta wp.")
	@In
	//@Unit("mm/min")
	public double thetaWp;
	
	@Description("Theta fc.")
	@In
	//@Unit("mm/min")
	public double thetaFc;
	// SOIL PARAMETERS
	@Description("The hydraulic conductivity at saturation")
	@In 
	@Unit ("m/s")
	public double[] ks;

	@Description("Saturated water content")
	@In 
	@Unit ("-")
	public double[] thetaS;

	@Description("Residual water content")
	@In 
	@Unit ("-")
	public double[] thetaR;

	@Description("First parameter of SWRC")
	@In 
	@Unit ("-")
	public double[] par1SWRC;

	@Description("Second parameter of SWRC")
	@In 
	@Unit ("-")
	public double[] par2SWRC;
	
	@Description("Third parameter of SWRC")
	@In 
	@Unit ("-")
	public double[] par3SWRC;
	
	@Description("Fourth parameter of SWRC")
	@In 
	@Unit ("-")
	public double[] par4SWRC;
	
	@Description("Fifth parameter of SWRC")
	@In 
	@Unit ("-")
	public double[] par5SWRC;
	
	@Description("Critical value of psi for which the moisture capacity is null")
	@In 
	@Unit ("m")
	public double[] psiStar1;
	
	@Description("Critical value of psi for which the moisture capacity is null")
	@In 
	@Unit ("m")
	public double[] psiStar2;
	
	@Description("Critical value of psi for which the moisture capacity is null")
	@In 
	@Unit ("m")
	public double[] psiStar3;
	
	@Description("Aquitard compressibility")
	@In 
	@Unit ("1/Pa")
	public double[] alphaSpecificStorage;

	@Description("Water compressibility")
	@In 
	@Unit ("1/Pa")
	public double[] betaSpecificStorage;

	@Description("It is possibile to chose between 3 different models to compute "
			+ "the soil hydraulic properties: Van Genuchten; Brooks and Corey; Kosugi unimodal")
	@In 
	public String soilHydraulicModel;

	@Description("Hydraulic conductivity at control volume interface can be evaluated as"
			+ " the average of kappas[i] and kappas[i+1]"
			+ " the maximum between kappas[i] and kappas[i+1]"
			+ " the minimum between kappas[i] and kappas[i+1]"
			+ " a weighted average of kappas[i] and kappas[i+1] where weights are dx[i] and dx[i+1]")
	@In
	public String interfaceHydraulicCondType;

	@Description("Number of Picard iteration to update the diffusive flux matrix")
	//@In
	public int picardIteration=1;
	/////////////////////////////////////////////

	@Description("Coefficient to simulate ET by making use of Casulli's formula")
	@In
	@Unit("1/s")
	public double[] et;

	@Description("Initial condition for water head read from grid NetCDF file")
	@In
	@Unit("m")
	public double[] psiIC;

	@Description("z coordinate read from grid NetCDF file")
	@In
	@Unit("m")
	public double[] z;

	@Description("Space delta to compute gradients read from grid NetCDF file")
	@In 
	@Unit("m")
	public double[] spaceDeltaZ;

	@Description("Length of control volumes read from grid NetCDF file")
	@In 
	@Unit("m")
	public double[] deltaZ;

	@Description("Time amount at every time-loop")
	@In
	@Unit ("s")
	public double tTimestep;

	@Description("Time step of integration")
	@In
	@Unit ("s")
	public double timeDelta;

	@Description("Control variable for the integration time loop ")
	@In
	@Unit ("s")
	public double sumTimeDelta;

	@Description("Tolerance for Newton iteration")
	@In
	public double newtonTolerance;

	@Description("Control parameter for nested Newton algorithm:"
			+"0 --> simple Newton method"
			+"1 --> nested Newton method")
	@In
	public int nestedNewton; 

	@Description("Slope of the soil")
	@In 
	@Unit ("�")
	public double delta;

	// BOUNDARY CONDITIONS

	@Description("The HashMap with the time series of the boundary condition at the top of soil column")
	@In
	@Unit ("m")
	public HashMap<Integer, double[]> inTopBC;

	@Description("It is possibile to chose between 2 different kind "
			+ "of boundary condition at the top of the domain: "
			+ "- Dirichlet boundary condition --> Top Dirichlet"
			+ "- Neumann boundary condition --> Top Neumann")
	@In 
	public String topBCType;

	@Description("The HashMap with the time series of the boundary condition at the bottom of soil column")
	@In
	@Unit ("m")
	public HashMap<Integer, double[]> inBottomBC;

	@Description("It is possibile to chose among 3 different kind "
			+ "of boundary condition at the bottom of the domain: "
			+ "- Dirichlet boundary condition --> Bottom Dirichlet"
			+ "- Neumann boundary condition --> Bottom Neumann"
			+ "- Impervious boundary condition --> Bottom Impervious")
	@In 
	public String bottomBCType;

	@Description("The first day of the simulation.")
	@In
	@Out
	public String inCurrentDate;

	@Description("Path of output files")
	@In
	public String dir;

	@Description("ArrayList of variable to be stored in the buffer writer")
	@Out
	public ArrayList<double[]> outputToBuffer;
	
	@Description("Run-off")
	@Unit("m/s")
	@Out
	public double runOff;


	//////////////////////////////////////////
	//////////////////////////////////////////

	@Description("Maximun number of Newton iterations")
	final int MAXITER_NEWT = 50;

	@Description("Top boundary condition according with topBCType")
	@Unit ("")
	double topBC;

	@Description("Bottom boundary condition according with bottomBCType")
	@Unit ("")
	double bottomBC;

	@Description("Top boundary condition according with topBCType")
	@Unit ("")
	double tBC;

	@Description("Bottom boundary condition according with topBCType")
	@Unit ("")
	double bBC;

	@Description("Psi values")
	@Unit ("m")
	double[] psis;

	@Description("Hydraulic conductivity at the bottom of the soil column")
	@Unit ("m/s")
	double k_b;

	@Description("Vector collects the hydraulic conductivity of each cell")
	@Unit ("m/s")
	double[] kappas;

	@Description("Vector collects the dimensional water content of each cell at time level n")
	@Unit ("-")
	double[] volumes;

	@Description("Vector collects the dimensional water content of each cell at time level n+1")
	@Out
	@Unit ("-")
	double[] volumesNew;

	@Description("Vector collects the adimensional water content of each soil cell and the water depth at soil surface at time level n+1")
	@Unit ("-")
	double[] thetasNew;

	@Description("Total volume at time level n")
	@Unit ("-")
	double volume;

	@Description("Total volume at time level n+1")
	@Unit ("-")
	double volumeNew;

	@Description("Volume error between time levels n+1 and n")
	@Unit ("-")
	double errorVolume;

	@Description("Vector collects velocities at cells' interfaces")
	@Unit ("m/s")
	double[] velocities;

	@Description("Vector collects the lower diagonal entries of the coefficient matrix")
	@Unit ("?")
	double[] lowerDiagonal;

	@Description("Vector collects the main diagonal entries of the coefficient matrix")
	@Unit ("?")
	double[] mainDiagonal;

	@Description("Vector collects the upper diagonal entries of the coefficient matrix")
	@Unit ("?")
	double[] upperDiagonal;

	@Description("Right hand side vector of the scalar equation to solve")
	@Unit ("-")
	double[] rhss;
	
	@Description("Water transpired")
	@In
	@Unit ("-")
	public double[] transpiredWater;
	
	@Description("Hydraulic conductivity at the cell interface i+1/2")
	@Unit ("m/s")
	double kP;

	@Description("Hydraulic conductivity at the cell interface i-1/2")
	@Unit ("m/s")
	double kM;

	@Description("Risidual of the outer iteration of Nested Newton method")
	double outerResidual;

	@Description("Risidual of the inner iteration of Nested Newton method")
	double innerResidual;

	@Description("Number of control volume for domain discetrization")
	@Unit (" ")
	int NUM_CONTROL_VOLUMES; 

	@Description("It is needed to iterate on the date")
	int step;

	@Description("Vector containing the z coordinates of the centres of control volumes")
	double[] zeta;

	@Description("Space step")
	@Out
	@Unit ("m")
	double[] spaceDelta;

	@Description("Vector containing the length of each control volume")
	@Out
	public double[] dx;

	double time=0;

	@Description("Variable containing theta-psi, hydraulic conductivity-psi, and moisture capacity-psi")
	LinkedHashMap<String,double[]> hydraulicParametrization;

	@Description("Object to perform the nested Newton algortithm")
	NestedNewton nestedNewtonAlg;

	@Description("Object dealing with SWRC model")
	SoilParametrization soilPar;
	
	@Description("Object to compute total water depth")
	TotalDepth totalDepth;

	@Description("This object compute the diagonal and right hand side entries for the uppermost cell accordingly with the prescribed top boundary condition.")
	BoundaryCondition topBoundaryCondition;
	
	@Description("This object compute the diagonal and right hand side entries for the lowermost cell accordingly with the prescribed bottom boundary condition.")
	BoundaryCondition bottomBoundaryCondition;

	WriteNetCDFRichardsParameterization writeSoilPar;
	
	@Description("This object compute hydraulic quantities.")
	ComputeDerivedQuantities compute;

	@Description("This object compute the interface hydraulic conductivity accordingly with the prescribed method.")
	InterfaceHydraulicConductivity interfaceHydraulicConductivity;
	double nullValue = -9999.0;

	///////////////////////////////

	@Execute
	public void solve() throws SchemaException {

		System.out.println("RICHARDS 1D "+inCurrentDate);

		if(step==0){


			NUM_CONTROL_VOLUMES = z.length;

			psis 		  = new double[NUM_CONTROL_VOLUMES];						
			k_b			  = 0.0;
			kappas 		  = new double[NUM_CONTROL_VOLUMES];
			volumes		  = new double[NUM_CONTROL_VOLUMES];
			volumesNew    = new double[NUM_CONTROL_VOLUMES];
			thetasNew     = new double[NUM_CONTROL_VOLUMES];
			velocities    = new double[NUM_CONTROL_VOLUMES+1];
			lowerDiagonal = new double[NUM_CONTROL_VOLUMES];
			mainDiagonal  = new double[NUM_CONTROL_VOLUMES];
			upperDiagonal = new double[NUM_CONTROL_VOLUMES];
			rhss 		  = new double[NUM_CONTROL_VOLUMES];
			transpiredWater	  = new double[NUM_CONTROL_VOLUMES];

			kP 		      = 0.0;
			kM	          = 0.0;
			zeta 		  = new double[NUM_CONTROL_VOLUMES];
			spaceDelta    = new double[NUM_CONTROL_VOLUMES];
			dx			  = new double[NUM_CONTROL_VOLUMES];
			outputToBuffer= new ArrayList<double[]>();
			System.out.println(NUM_CONTROL_VOLUMES);

			writeSoilPar = new WriteNetCDFRichardsParameterization();

			SimpleSoilParametrizationFactory soilParFactory = new SimpleSoilParametrizationFactory();
			//soilPar = soilParFactory.createSoilParametrization(soilHydraulicModel,alpha,n,psiE,lambda,rMedian,sigma,thetaR,thetaS,ks);
			soilPar = soilParFactory.createSoilParametrization(soilHydraulicModel);
			soilPar.set(par1SWRC, par2SWRC, par3SWRC, par4SWRC, par5SWRC, psiStar1, psiStar2, psiStar3, alphaSpecificStorage, betaSpecificStorage, thetaR, thetaS, ks);
			totalDepth = new TotalDepth();


			SimpleBoundaryConditionFactory boundCondFactory = new SimpleBoundaryConditionFactory();
			topBoundaryCondition = boundCondFactory.createBoundaryCondition(topBCType);		
			bottomBoundaryCondition = boundCondFactory.createBoundaryCondition(bottomBCType);	

			SimpleInterfaceHydraulicConductivityFactory interfaceHydraulicCondFactory = new SimpleInterfaceHydraulicConductivityFactory();
			interfaceHydraulicConductivity = interfaceHydraulicCondFactory.createInterfaceHydraulicConductivity(interfaceHydraulicCondType);

			for(int i = 0; i < NUM_CONTROL_VOLUMES; i++) {
				psis[i] = psiIC[i];
				zeta[i] = z[i];
				spaceDelta[i] = spaceDeltaZ[i];
			}
			for(int i = 0; i < NUM_CONTROL_VOLUMES-1; i++) {
				dx[i] = deltaZ[i];
				
			}

			compute = new ComputeDerivedQuantities(NUM_CONTROL_VOLUMES, dx, spaceDelta, soilPar, totalDepth, interfaceHydraulicConductivity, bottomBCType);

			//nestedNewtonAlg = new NestedNewton(nestedNewton, newtonTolerance, MAXITER_NEWT, NUM_CONTROL_VOLUMES, dx, soilPar, totalDepth, par1SWRC, par2SWRC, par3SWRC, par4SWRC, par5SWRC, alphaSpecificStorage, betaSpecificStorage, thetaR, thetaS);
			nestedNewtonAlg = new NestedNewton(nestedNewton, newtonTolerance, MAXITER_NEWT, NUM_CONTROL_VOLUMES, dx, soilPar, totalDepth);

			// conversion from degree to radiant of slope angle
			delta = delta*Math.PI/180;

			// Create and print a matrxi with data necessary to plot SWRC, hydraulic conductivity and moisture capacity parametrization
			//hydraulicParametrization = soilPar.hydraulicModelCurves1();
			//writeSoilPar.writeNetCDF(hydraulicParametrization, dir+"/HydraulicParameterization", soilHydraulicModel);
			//psis[320] = 0.6;
		} // chiudi step==0

		double transpiration = inTranspiration.get(1)[0];
		if (transpiration == (nullValue)) {transpiration = 0;}		
		   	
		//time = time + tTimestep;

		topBC = 0.0;
		topBC = (inTopBC.get(1)[0]/1000)/tTimestep;

		bottomBC = 0.0;
		if(inBottomBC != null)
			bottomBC = inBottomBC.get(1)[0];
		if(bottomBCType.equalsIgnoreCase("Bottom Neumann") || bottomBCType.equalsIgnoreCase("BottomNeumann")) {
			bottomBC = bottomBC/tTimestep;
		}

		outputToBuffer.clear();

		sumTimeDelta = 0;
		while(sumTimeDelta < tTimestep) {

			if(sumTimeDelta + timeDelta>tTimestep) {
				timeDelta = tTimestep - sumTimeDelta;
			}
			sumTimeDelta = sumTimeDelta + timeDelta;

			for(int picard=0; picard<picardIteration; picard++) {
				/*
				for(int i = 0; i < NUM_CONTROL_VOLUMES; i++) {  
					if(i==0) {
						//soilPar.set(par1SWRC[i],par2SWRC[i],par3SWRC[i],par4SWRC[i],par5SWRC[i],alphaSpecificStorage[i],betaSpecificStorage[i],thetaR[i],thetaS[i],ks[i]);
						volumes[i] = soilPar.waterContent(psis[i],i)*dx[i];
						kappas[i] = soilPar.hydraulicConductivity(psis[i],i);
						k_b = soilPar.hydraulicConductivity(bottomBC,i);  // I use the same parameters of the bottom cell
					} else if(i==NUM_CONTROL_VOLUMES-1) {
						volumes[i] = totalDepth.totalDepth(psis[i]);
						//soilPar.set(par1SWRC[i-1],par2SWRC[i-1],par3SWRC[i-1],par4SWRC[i-1],par5SWRC[i-1],alphaSpecificStorage[i-1],betaSpecificStorage[i-1],thetaR[i-1],thetaS[i-1],ks[i-1]);
						kappas[i] = soilPar.hydraulicConductivity(psis[i],i-1);
					} else {
						//soilPar.set(par1SWRC[i],par2SWRC[i],par3SWRC[i],par4SWRC[i],par5SWRC[i],alphaSpecificStorage[i],betaSpecificStorage[i],thetaR[i],thetaS[i],ks[i]);
						volumes[i] = soilPar.waterContent(psis[i],i)*dx[i];
						kappas[i] = soilPar.hydraulicConductivity(psis[i],i);
					}
				}
				*/
				//soilPar.set(par1SWRC[0],par2SWRC[0],par3SWRC[0],par4SWRC[0],par5SWRC[0],alphaSpecificStorage[0],betaSpecificStorage[0],thetaR[0],thetaS[0],ks[0]);
				k_b = soilPar.hydraulicConductivity(bottomBC,0);
				// Compute volumes and hydraulic conductivity

				compute.setComputeDerivedQuantities(psis, kappas, bottomBC, k_b);
				volumes = compute.computeWaterVolumes().clone();
				kappas = compute.computeKappas().clone();
				//soilPar.set(par1SWRC[0],par2SWRC[0],par3SWRC[0],par4SWRC[0],par5SWRC[0],alphaSpecificStorage[0],betaSpecificStorage[0],thetaR[0],thetaS[0],ks[0]);
				//k_b = soilPar.hydraulicConductivity(bottomBC);  // I use the same parameters of the bottom cell


				/* COEFFICIENT MATRIX IS BUILD BY THREE VECTORS COLLECTING ELEMENTS OF THE THREE DIAGONAL:
				   	 a lower diagonal psi_(i+1)
				   	 b main diagonal  psi_i
				   	 c upper diagonal psi_(i-1)
				   	 RIGHT HAND SIDE */
				for(int i = 0; i < NUM_CONTROL_VOLUMES; i++) {
					if( i == 0 ) {

						kP = interfaceHydraulicConductivity.compute(kappas[i],kappas[i+1],dx[i],dx[i+1]);
						if(bottomBCType.equalsIgnoreCase("Bottom Free Drainage") || bottomBCType.equalsIgnoreCase("BottomFreeDrainage")){
							
							kM = kappas[i];
						
						} else if (bottomBCType.equalsIgnoreCase("Bottom Neumann") || bottomBCType.equalsIgnoreCase("BottomNeumann")) {
							kM = -999;
						}
						else {

							kM = interfaceHydraulicConductivity.compute(kappas[i],k_b,dx[i],dx[i]);
						}
						
						lowerDiagonal[i] =  bottomBoundaryCondition.lowerDiagonal(-999, kP, kM, spaceDelta[i+1], spaceDelta[i], timeDelta, delta);
						mainDiagonal[i] = bottomBoundaryCondition.mainDiagonal(-999, kP, kM, spaceDelta[i+1], spaceDelta[i], timeDelta, delta);
						upperDiagonal[i] = bottomBoundaryCondition.upperDiagonal(-999, kP, kM, spaceDelta[i+1], spaceDelta[i], timeDelta, delta);
						rhss[i] = volumes[i]  + bottomBoundaryCondition.rightHandSide(bottomBC, kP, kM, spaceDelta[i+1], spaceDelta[i], timeDelta, delta) - timeDelta*et[i]*(volumes[i] - thetaR[i]*dx[i]);


					} else if(i == NUM_CONTROL_VOLUMES -1) {
						
						kP = 0;	
						kM = interfaceHydraulicConductivity.compute(kappas[i],kappas[i-1],dx[i],dx[i-1]);
						lowerDiagonal[i] = topBoundaryCondition.lowerDiagonal(-999, kP, kM, spaceDelta[i], spaceDelta[i-1], timeDelta, delta); 
						mainDiagonal[i] = topBoundaryCondition.mainDiagonal(-999, kP, kM, spaceDelta[i], spaceDelta[i-1], timeDelta, delta);
						upperDiagonal[i] = topBoundaryCondition.upperDiagonal(-999, kP, kM,  spaceDelta[i], spaceDelta[i-1], timeDelta, delta);
						rhss[i] = volumes[i] + topBoundaryCondition.rightHandSide(topBC, kP, kM,  spaceDelta[i], spaceDelta[i-1], timeDelta, delta) - timeDelta*et[i]*(volumes[i]);


					} else {

						kP = interfaceHydraulicConductivity.compute(kappas[i],kappas[i+1],dx[i],dx[i+1]);
						kM = interfaceHydraulicConductivity.compute(kappas[i],kappas[i-1],dx[i],dx[i-1]);
						lowerDiagonal[i] = -kM*timeDelta/spaceDelta[i]*1/Math.pow(Math.cos(delta),2);
						mainDiagonal[i] = kM*timeDelta/spaceDelta[i]*1/Math.pow(Math.cos(delta),2)  + kP*timeDelta/spaceDelta[i+1]*1/Math.pow(Math.cos(delta),2);
						upperDiagonal[i] = -kP*timeDelta/spaceDelta[i+1]*1/Math.pow(Math.cos(delta),2);
						rhss[i] = volumes[i] + timeDelta*(kP - kM) - timeDelta*et[i]*(volumes[i] - thetaR[i]*dx[i]);
					}

				}
				System.out.println("");
				//// NESTED NEWTON ALGORITHM ////
				nestedNewtonAlg.set(psis, mainDiagonal, upperDiagonal, lowerDiagonal, rhss);
				psis = nestedNewtonAlg.solver();

				/* COMPUTE velocities AT CELL INTERFACES at time level n+1
				 * with hydraulic conductivity at time level n 
				 */ 
				volume = 0.0;
				volumeNew =0.0;
			
				double rootsDepth = 0.3;
				int numberOfLayers = z.length;
				double totalDepth=z[NUM_CONTROL_VOLUMES-1];
				double transpiredWater = transpiration/rootsDepth;
				double[] layers = new double[NUM_CONTROL_VOLUMES];
				for(int i = 0; i < NUM_CONTROL_VOLUMES; i++) {
					layers[i] = dx[i];				}

				double[] transpiredWaterForLayers = new double[numberOfLayers];
				for(int i = 0; i <NUM_CONTROL_VOLUMES; i++) {
					if( totalDepth < rootsDepth ) {
						transpiredWaterForLayers[i] = transpiredWater * dx[i];
						}
						else {
							transpiredWaterForLayers[i] = 0;
						}
					totalDepth = totalDepth-layers[i];
				}
			
				compute.setComputeDerivedQuantities(psis, kappas, bottomBC, k_b);
				double[] stressFactor = new double[NUM_CONTROL_VOLUMES]; 
				velocities = compute.computeVelocities().clone();
				volumesNew = compute.computeWaterVolumes().clone();
				for(int i = 0; i < NUM_CONTROL_VOLUMES-2; i++) {
					stressFactor[i] = (volumesNew[i]/dx[i] - thetaWp)/ (thetaFc - thetaWp);
					stressFactor[i] = ((stressFactor[i]>0)?stressFactor[i]:0);
					double minValue = volumesNew[i]-thetaR[i]*dx[i];
					transpiredWaterForLayers[i] = transpiredWaterForLayers[i] * stressFactor[i];
					transpiredWaterForLayers[i] = ((transpiredWaterForLayers[i]<minValue)?transpiredWaterForLayers[i]:minValue);
					volumesNew[i] = volumesNew[i] - transpiredWaterForLayers[i];
					thetasNew[i]=volumesNew[i]/dx[i]; 
					}
				volume = compute.computeTotalWaterVolumes(volumes);
				stressFactor[1] = ((stressFactor[1]<0)?0:stressFactor[1]);
				double outET = transpiredWater*stressFactor[1];

				System.out.println("ET stress			"+outET);
			
				volumeNew = compute.computeTotalWaterVolumes(volumesNew);
				//thetasNew = compute.computeThetas().clone();
				errorVolume = volumeNew - volume - timeDelta*(topBC + velocities[0]);
				
			}

		}
		
		outputToBuffer.add(psis);
		outputToBuffer.add(thetasNew);
		outputToBuffer.add(psiIC);
		outputToBuffer.add(velocities);
		outputToBuffer.add(new double[] {errorVolume});
		outputToBuffer.add(new double[] {topBC*tTimestep*1000}); // I want to have rainfall height instead of water flux
		if(bottomBCType.equalsIgnoreCase("Bottom Neumann") || bottomBCType.equalsIgnoreCase("BottomNeumann")) {
			bottomBC = bottomBC*tTimestep;
		}
		outputToBuffer.add(new double[] {bottomBC});
		runOff = topBC+velocities[NUM_CONTROL_VOLUMES];
		outputToBuffer.add(new double[] {runOff});



		step++;

	} //// MAIN CYCLE END ////
	/// CLOSE Richards1d ///
	}