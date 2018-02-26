package report;

import core.ConnectionListener;
import core.DTNHost;
import core.SimClock;

import java.util.*;
import java.util.function.BiConsumer;


public class InterGroupContacts extends Report implements ConnectionListener {

	protected HashMap<String, Integer> groupContacts;

	public InterGroupContacts() {
		init();
	}

	@Override
	public void init() {
		super.init();
		groupContacts = new HashMap<>();
		groupContacts.put("CafeteriaIntern", 0);
		groupContacts.put("ChairIntern", 0);
		groupContacts.put("FacilityManagementIntern", 0);
		groupContacts.put("LibraryIntern", 0);
		groupContacts.put("StudentIntern", 0);
		groupContacts.put("CafeteriaChair", 0);
		groupContacts.put("CafeteriaFacilityManagement", 0);
		groupContacts.put("CafeteriaLibrary", 0);
		groupContacts.put("CafeteriaStudent", 0);
		groupContacts.put("ChairFacilityManagement", 0);
		groupContacts.put("ChairLibrary", 0);
		groupContacts.put("ChairStudent", 0);
		groupContacts.put("FacilityManagementLibrary", 0);
		groupContacts.put("FacilityManagementStudent", 0);
		groupContacts.put("LibraryStudent", 0);
	}

	public void hostsConnected(DTNHost host1, DTNHost host2) {

		String key = findInterGroup(host1, host2);
		if (key.equals("")) key = findInterGroup(host2, host1);
		if (key.equals("")) System.out.println("Error Key does not exist for host " +  host1.getGroupID() + " and " + host2.getGroupID());
		increaseCount(key);

	}

	public void hostsDisconnected(DTNHost host1, DTNHost host2) {
		// Do nothing
	}

	public void done() {
		BiConsumer<String, Integer> bC = (String groupConstellation, Integer count) -> write(groupConstellation +"\t" + count);
		groupContacts.forEach(bC);
		super.done();
	}

	private String findInterGroup (DTNHost h1, DTNHost h2) {
		String key = "";
		switch (h1.getGroupID()) {
			case "Caf":
				switch (h2.getGroupID()) {
					case "Caf":
						key = "CafeteriaIntern";
						break;
					case "Chair":
						key = "CafeteriaChair";
						break;
					case "FM":
						key = "CafeteriaFacilityManagement";
						break;
					case "L":
						key = "CafeteriaLibrary";
						break;
					case "Student":
						key = "CafeteriaStudent";
						break;
					default:
						break;
				}
				break;
			case "Chair":
				switch (h2.getGroupID()) {
					case "Chair":
						key = "ChairIntern";
						break;
					case "FM":
						key = "ChairFacilityManagement";
						break;
					case "L":
						key = "ChairLibrary";
						break;
					case "Student":
						key = "ChairStudent";
						break;
					default:
						break;
				}
				break;
			case "FM":
				switch (h2.getGroupID()) {
					case "FM":
						key = "FacilityManagementIntern";
						break;
					case "L":
						key = "FacilityManagementLibrary";
						break;
					case "Student":
						key = "FacilityManagementStudent";
						break;
					default:
						break;
				}
				break;
			case "L":
				switch (h2.getGroupID()) {
					case "L":
						key = "LibraryIntern";
						break;
					case "Student":
						key = "LibraryStudent";
						break;
					default:
						break;
				}
				break;
			case "Student":
				if (h2.getGroupID().equals("Student")) key = "StudentIntern";
				break;
			default:
				break;
		}
		//System.out.println("Combination " + h1.getGroupID() + " and " + h2.getGroupID() + " result in key " + key);
		return key;
	}
	private void increaseCount (String key) {
		if (groupContacts.containsKey(key)){
			groupContacts.put(key,groupContacts.get(key)+1);
		} else {
		}
	}

}
