/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package source;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.Scanner;
import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.CloudletSchedulerDynamicWorkload;
import org.cloudbus.cloudsim.DatacenterBroker;
import org.cloudbus.cloudsim.DatacenterCharacteristics;
import org.cloudbus.cloudsim.HostStateHistoryEntry;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.Pe;
import org.cloudbus.cloudsim.Storage;
import org.cloudbus.cloudsim.UtilizationModelStochastic;
import org.cloudbus.cloudsim.Vm;
import org.cloudbus.cloudsim.VmSchedulerTimeShared;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.power.PowerDatacenter;
import org.cloudbus.cloudsim.power.PowerHost;
import org.cloudbus.cloudsim.power.models.PowerModelLinear;
import org.cloudbus.cloudsim.provisioners.BwProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.PeProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.RamProvisionerSimple;
import org.cloudbus.cloudsim.power.PowerVmAllocationPolicyMigrationAbstract;
import org.cloudbus.cloudsim.power.PowerVmAllocationPolicyMigrationInterQuartileRange;
import org.cloudbus.cloudsim.power.PowerVmAllocationPolicyMigrationLocalRegression;
import org.cloudbus.cloudsim.power.PowerVmAllocationPolicyMigrationLocalRegressionRobust;
import org.cloudbus.cloudsim.power.PowerVmAllocationPolicyMigrationMedianAbsoluteDeviation;
import org.cloudbus.cloudsim.power.PowerVmAllocationPolicyMigrationStaticThreshold;
import org.cloudbus.cloudsim.power.PowerVmAllocationPolicySimple;
import org.cloudbus.cloudsim.power.PowerVmSelectionPolicy;
import org.cloudbus.cloudsim.power.PowerVmSelectionPolicyMaximumCorrelation;
import org.cloudbus.cloudsim.power.PowerVmSelectionPolicyMinimumMigrationTime;
import org.cloudbus.cloudsim.power.PowerVmSelectionPolicyMinimumUtilization;
import org.cloudbus.cloudsim.power.PowerVmSelectionPolicyRandomSelection;
/**
 *
 * @author ben
 */
public class source {
    /**
     * @param args the command line arguments
     */
    private static List<Cloudlet> cloudletList;
    private static List<Vm> vmList;
    private static double hostsNumber = 2;
    private static int vmsNumber = 20;
    private static int cloudletsNumber = 25;
    static double [][] tree=new double[2][cloudletsNumber+1];
    static int count_tree=1;
    static int generation=10;
    static double en_Threashold,en_Threashold1;
    
    public static void main(String[] args) {
        Random x = new Random();
        double k=0;
        double[] d=new double[3];

        for(int j=0;j<cloudletsNumber;j++){
            tree[0][j] = x.nextInt(vmsNumber);
        }
        d = cost(0);
        en_Threashold = d[0];
            
        for (int i = 0; i < generation; i++) {
            //------------------vm migration ----------------------------
            for(int j=0;j<cloudletsNumber;j++){
              tree[1][j]=x.nextInt(vmsNumber);
            }
            d= cost(1);
            en_Threashold1=d[0];
            //--------------------------------------------
            if(en_Threashold1<en_Threashold){
                en_Threashold = en_Threashold1;
                vm_replacment();
            }
        }
        printCloudletList(d);
    }
    
    private static void vm_replacment(){
        for(int j=0;j<cloudletsNumber;j++){
              tree[0][j]=tree[1][j];
          }
    }
    
    static double[] thershold =new double[vmsNumber]; 
    static long[] length_task=new long[cloudletsNumber];  
    static long[] fileSize_task = new long[cloudletsNumber];  
    static long[] outputSize_task = new long[cloudletsNumber];  
    public static double en;

    private static double[] cost(int id){
        double[] d=new double[3];
        try {
            int num_user = 1; 
            Calendar calendar = Calendar.getInstance();
            boolean trace_flag = false; 

            create_task();
            CloudSim.init(num_user, calendar, trace_flag);

            PowerDatacenter datacenter = createDatacenter("Datacenter_0");

            DatacenterBroker broker = createBroker();
            int brokerId = broker.getId();

            vmList = createVms(brokerId);

            broker.submitVmList(vmList);

            cloudletList = createCloudletList(brokerId,id);

            broker.submitCloudletList(cloudletList);

            double lastClock = CloudSim.startSimulation();

            List<Cloudlet> newList = broker.getCloudletReceivedList();


            CloudSim.stopSimulation();
             int totalTotalRequested = 0;
            double totalTotalAllocated = 0;
            int numberOfAllocations = 0;
            double  alloc;
            int count=0;
            for(PowerHost h : datacenter.<PowerHost>getHostList()){ // Get every host
                numberOfAllocations=0;
                double totalAllocated = 0;
                for (HostStateHistoryEntry entry : h.getStateHistory()){ // Get host state history
                        alloc = entry.getAllocatedMips();
                        numberOfAllocations++;
                        totalAllocated += alloc;
                }
                totalTotalAllocated += totalAllocated/numberOfAllocations;
            }
            d[0]=datacenter.getPower() / (3600 * 1000);
            d[1]=lastClock;
            d[2]=totalTotalAllocated;
            return d;
	}catch (Exception e) {
            d[0]=100000;
            d[1]=100000;
            d[2]=100000;
            return d;
        }
    }
        
    private static List<Cloudlet> createCloudletList(int brokerId,int id){
        List<Cloudlet> list = new ArrayList<Cloudlet>();
        int pesNumber=1;       
        for (int i = 0; i < cloudletsNumber; i++) {
            Cloudlet cloudlet = new Cloudlet(i, length_task[i], pesNumber, fileSize_task[i], outputSize_task[i], new UtilizationModelStochastic(), new UtilizationModelStochastic(), new UtilizationModelStochastic());
            cloudlet.setUserId(brokerId);
            cloudlet.setVmId((int)tree[id][i]);
            list.add(cloudlet);
        }
        return list;
    }

    private static List<Vm> createVms(int brokerId){
        List<Vm> vms = new ArrayList<Vm>();
        int[] mips = { 250, 350, 375, 100 }; 
        int pesNumber = 1;
        int ram = 512;
        long bw = 25; 
        long size = 102400;
        String vmm = "Xen";
        for (int i = 0; i < vmsNumber; i++) {
            vms.add(new Vm(i, brokerId, mips[i % mips.length], pesNumber, ram, bw, size, vmm, new CloudletSchedulerDynamicWorkload(mips[i % mips.length], pesNumber)));
        }
        return vms;
    }
 
    private static void create_task() {
        length_task[0] = 2160657; 
        length_task[1] = 1835957; 
        length_task[2] = 1819923; 
        length_task[3] = 1747767; 
        length_task[4] = 1599447; 
        length_task[5] = 1583413; 
        length_task[6] = 1607465; 
        length_task[7] = 1427076; 
        length_task[8] = 1463154; 
        length_task[9] = 1447119; 
        length_task[10] =1527292; 
        length_task[11] =1503240; 
        length_task[12] =1495223; 
        length_task[13] =1362938; 
        length_task[14] =1370955; 
        length_task[15] =1378972; 
        length_task[16] =1471171; 
        length_task[17] =1431084; 
        length_task[18] =1439102; 
        length_task[19] =1419059; 
        length_task[20] =1403024; 
        length_task[21] =1407033; 
        length_task[22] =1407033; 
        length_task[23] =1423067; 
        length_task[24] =1419059; 
                
//------------------------------------------------------------------------------		
        fileSize_task[0] = 291738; 
        fileSize_task[1] = 291738; 
        fileSize_task[2] = 291738; 
        fileSize_task[3] = 291738; 
        fileSize_task[4] = 291738; 
        fileSize_task[5] = 291738; 
        fileSize_task[6] = 291738; 
        fileSize_task[7] = 291738; 
        fileSize_task[8] = 291738; 
        fileSize_task[9] = 291738; 
        fileSize_task[10] = 291738; 
        fileSize_task[11] = 291738; 
        fileSize_task[12] = 291738; 
        fileSize_task[13] = 291738; 
        fileSize_task[14] = 291738; 
        fileSize_task[15] = 291738; 
        fileSize_task[16] = 291738; 
        fileSize_task[17] = 291738; 
        fileSize_task[18] = 291738; 
        fileSize_task[19] = 291738; 
        fileSize_task[20] = 291738; 
        fileSize_task[21] = 291738; 
        fileSize_task[22] = 291738; 
        fileSize_task[23] = 291738; 
        fileSize_task[24] = 291738; 
//------------------------------------------------------------------------------                
        outputSize_task[0] = 5662310; 
        outputSize_task[1] = 5662310; 
        outputSize_task[2] = 5662310;  
        outputSize_task[3] = 5662310;  
        outputSize_task[4] = 5662310;  
        outputSize_task[5] = 5662310;  
        outputSize_task[6] = 5662310;  
        outputSize_task[7] = 5662310; 
        outputSize_task[8] = 5662310;  
        outputSize_task[9] = 5662310;  
        outputSize_task[10] = 5662310; 
        outputSize_task[11] = 5662310; 
        outputSize_task[12] = 5662310;  
        outputSize_task[13] = 5662310;  
        outputSize_task[14] = 5662310;  
        outputSize_task[15] = 5662310;  
        outputSize_task[16] = 5662310;  
        outputSize_task[17] = 5662310; 
        outputSize_task[18] = 5662310;  
        outputSize_task[19] = 5662310;  
        outputSize_task[20] = 5662310;  
        outputSize_task[21] = 5662310;  
        outputSize_task[22] = 5662310;  
        outputSize_task[23] = 5662310;  
        outputSize_task[24] = 5662310;
    }
    
    public static int max_host_cpu=0;
    public static int f_m=0;
    
    private static PowerDatacenter createDatacenter(String name) throws Exception{
        List<PowerHost> hostList = new ArrayList<PowerHost>();
        double maxPower = 4008; 
        double staticPowerPercent = 0.7; 
        int[] mips = { 1000, 2000, 3000 };
        int ram = 4096;; 
        long storage = 409600;
        int bw = 100;
        for (int i = 0; i < hostsNumber; i++) {
            List<Pe> peList = new ArrayList<Pe>();
            peList.add(new Pe(0, new PeProvisionerSimple(mips[i % mips.length]))); 
            hostList.add(new PowerHost(
                                i,
                                new RamProvisionerSimple(ram),
                                new BwProvisionerSimple(bw),
                                storage,
                                peList,
                                new VmSchedulerTimeShared(peList), 
                                new PowerModelLinear(maxPower, staticPowerPercent))); 
        }
        String arch = "x86"; 
        String os = "Linux"; 
        String vmm = "Xen";
        double time_zone = 10.0;
        double cost = 3.0; 
        double costPerMem = 0.05;
        double costPerStorage = 0.001;
        double costPerBw = 0.0; 
        DatacenterCharacteristics characteristics = new DatacenterCharacteristics(
                        arch, os, vmm, hostList, time_zone, cost, costPerMem, costPerStorage, costPerBw);
        PowerDatacenter powerDatacenter = null;
        try {
            powerDatacenter = new PowerDatacenter(
                            name,
                            characteristics,
                            new PowerVmAllocationPolicySimple(hostList),
                            new LinkedList<Storage>(),
                            5.0);
        }catch (Exception e){
            e.printStackTrace();
        }
        return powerDatacenter;
    }

    private static DatacenterBroker createBroker() {
        DatacenterBroker broker = null;
        try {
            broker = new DatacenterBroker("Broker");
        }catch (Exception e){
                e.printStackTrace();
                return null;
        }
        return broker;
    }
        
    private static void printCloudletList(double[] d) {
        String indent = "\t";
        Log.printLine();
        Log.printLine("========== OUTPUT ==========");
        Log.printLine("Cloudlet ID" + indent + "VM ID"  );
        DecimalFormat dft = new DecimalFormat("###############.####");
        for (int i = 0; i < cloudletsNumber; i++){
            Log.printLine(  i+ indent+ indent + indent+tree[0][i]);
        }
        Log.printLine();
        Log.printLine(String.format("total Energy consumption: %f kWh",d[0]));
        Log.printLine(String.format("Total simulation time: %.2f sec",d[1]));
        Log.printLine(String.format("SLA: %f kWh",d[2]));
    }
}
