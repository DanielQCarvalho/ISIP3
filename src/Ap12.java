/** 
ISEL - DEETC
Introdução a Sistemas de Informação
MP,ND, 2014-2022
*/

import javax.swing.*;
import javax.swing.plaf.TextUI;
import javax.swing.plaf.nimbus.State;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;
import java.sql.*;
import java.util.*;


interface DbWorker 
{
	void doWork();
}	
class App
{
	private enum Option
	{
		Unknown,
		Exit,
		ListDepartment,
		ListEmployee,
		ListManagerDepartment,
		RegisterDepartment
	}
	private static App __instance = null;
	private String __connectionString;
	private HashMap<Option,DbWorker> __dbMethods;
	private static final String SELECT_CMD = "select * from departament";
	
	private App()
	{
		__dbMethods = new HashMap<Option,DbWorker>();
		__dbMethods.put(Option.ListDepartment, new DbWorker() {public void doWork() {App.this.ListDepartment();}});
		__dbMethods.put(Option.ListEmployee, new DbWorker() {public void doWork() {App.this.ListEmployee();}});
		__dbMethods.put(Option.ListManagerDepartment, new DbWorker() {public void doWork() {App.this.ListManagerDepartment();}});
		__dbMethods.put(Option.RegisterDepartment, new DbWorker() {public void doWork() {App.this.RegisterDepartment();}});

	}
	public static App getInstance() 
	{
		if(__instance == null) 
		{
			__instance = new App();
		}
		return __instance;
	}

	private Option DisplayMenu()
	{ 
		Option option=Option.Unknown;
		try
		{
			System.out.println("Course management");
			System.out.println();
			System.out.println("1. Exit");
			System.out.println("2. List departaments");
			System.out.println("3. List employees who have a higher than average annual salary in your department");
			System.out.println("4. List employees who are department heads and have no dependents");
			System.out.println("5. Register a novel department");
			System.out.print(">");
			Scanner s = new Scanner(System.in);
			int result = s.nextInt();
			option = Option.values()[result];			
		}
		catch(RuntimeException ex)
		{
			//nothing to do. 
		}
		
		return option;
		
	}
	private final static void clearConsole() throws Exception
	{
	    for (int y = 0; y < 25; y++) //console is 80 columns and 25 lines
	    System.out.println("\n");

	}
	private void Login() throws SQLException
	{

		Connection con = DriverManager.getConnection(getConnectionString());
		if(con != null)
			con.close();      
		
	}
	public void Run() throws Exception
	{
		Login ();
		Option userInput = Option.Unknown;
		do
		{
			clearConsole();
			userInput = DisplayMenu();
			clearConsole();		  	
			try
			{		
				__dbMethods.get(userInput).doWork();		
				System.in.read();		
				
			}
			catch(NullPointerException ex)
			{
				//Nothing to do. The option was not a valid one. Read another.
			}
			
		}while(userInput!=Option.Exit);
	}

	public String getConnectionString() 
	{
		return __connectionString;			
	}
	public void setConnectionString(String s) 
	{
		__connectionString = s;
	}

	/**
		To implement from this point forward. Do not need to change the code above.
	-------------------------------------------------------------------------------		
		IMPORTANT:
	--- DO NOT MOVE IN THE CODE ABOVE. JUST HAVE TO IMPLEMENT THE METHODS BELOW ---
	-------------------------------------------------------------------------------
	
	*/

	private void printResults(ResultSet dr, String query)
	{
		String[] columnNames = query.split(" ");
		List<String> listOfStrings = new ArrayList<>(Arrays.asList(columnNames));
		listOfStrings.removeIf( b -> Objects.equals(b, ",") ||
				listOfStrings.indexOf(b) == 0 || listOfStrings.indexOf(b) >= listOfStrings.indexOf("from"));

		System.out.println(listOfStrings);

		Formatter fmt = new Formatter();
		fmt.format("%15s\n", listOfStrings);

		try{
			int count = 0;
			while (dr.next()) {
				while(count != listOfStrings.size()){
					System.out.print(dr.getString(listOfStrings.get(count)));
					System.out.print(" ");
					count += 1;
				}
				System.out.println();
				count = 0;
			}
		}catch (Exception e){
			System.out.println(e);
		}
		//TODO
		/*Result must be similar like:
		ListDepartment()
		dname   		dnumber		mgrssn      mgrstartdate            
		-----------------------------------------------------
		Research		5  			333445555 	1988-05-22            
		Administration	4    		987654321	1995-01-01
	 */ 
	}
	private void ListDepartment()
	{
		try {
			Connection conn = DriverManager.getConnection(__connectionString);
			Statement stmt = conn.createStatement();

			String query = "select dname , dnumber , mgrssn , mgrstartdate from department";
			ResultSet list = stmt.executeQuery(query);
			printResults(list, query);
		}catch (Exception e){
			System.out.println(e);
		}
	}
	private void ListEmployee()
	{
		try {
			Connection conn = DriverManager.getConnection(__connectionString);
			Statement stmt = conn.createStatement();

			String query = "select fname , lname from employee,(select dnumber,avg(salary) as averageSalary\n" +
					"from department, employee\n" +
					"where employee.dno = department.dnumber\n" +
					"group by dnumber) as average\n" +
					"where employee.dno = average.dnumber and employee.salary > average.averageSalary";
			ResultSet list = stmt.executeQuery(query);
			printResults(list, query);
		}catch (Exception e){
			System.out.println(e);
		}
	}
	private void ListManagerDepartment()
	{
		try {
			Connection conn = DriverManager.getConnection(__connectionString);
			Statement stmt = conn.createStatement();

			String query = "select fname , lname from employee, (select department.mgrssn\n" +
					"from department, employee\n" +
					"join department dep2 on employee.ssn = dep2.mgrssn\n" +
					"except (select dependent.essn from dependent)) as ded2mde\n" +
					"where ssn = mgrssn";
			ResultSet list = stmt.executeQuery(query);
			printResults(list, query);
		}catch (Exception e){
			System.out.println(e);
		}
	}
	private void RegisterDepartment()
	{
		try {
			Connection conn = DriverManager.getConnection(__connectionString);
			Statement stmt = conn.createStatement();

			String query = "";
			int list = stmt.executeUpdate(query); //Update retorna nada logo = 0
		}catch (Exception e){
			System.out.println(e);
		}
	}
	
}

public class Ap12
{
	public static void main(String[] args) throws SQLException,Exception
	{
		String url = "jdbc:postgresql://10.62.73.73:5432/?user=mp38&password=mp38&ssl=false";
		App.getInstance().setConnectionString(url);
		App.getInstance().Run();
	}
}

/* -------------------------------------------------------------------------------- 
private class Connect {
	private java.sql.Connection con = null;
    private final String url = "jdbc:sqlserver://";
    private final String serverName = "localhost";
    private final String portNumber = "1433";
    private final String databaseName = "aula12";
    private final String userName = "xxxx";
    private final String password = "xxxxxxx";

    // Constructor
    public Connect() {
    }

    private java.sql.Connection getConnection() {
        try {
            con = java.sql.DriverManager.getConnection(url, user, pwd);
            if (con != null) {
                System.out.println("Connection Successful!");
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Error Trace in getConnection() : " + e.getMessage());
        }
        return con;
    }

    private void closeConnection() {
        try {
            if (con != null) {
                con.close();
            }
            con = null;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
 --------------------------------------------------------------------------------
 */