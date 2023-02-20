package javaInterface; /**
ISEL - DEETC
Introdução a Sistemas de Informação
MP,ND, 2014-2022
*/

import java.sql.*;
import java.util.*;

import static java.lang.Integer.parseInt;


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
		InsertDriver,
		VehicleOutdated,
		VehicleTotal,
		MostViagens,
		NoViagensCondutores,
		ViagensProprietario,
		ViagensCondutores,
	}
	private static App __instance = null;
	private String __connectionString;
	private HashMap<Option,DbWorker> __dbMethods;
	private static final String SELECT_CMD = "select * from departament";
	Data Data = new Data();
	
	private App()
	{
		__dbMethods = new HashMap<Option,DbWorker>();
		__dbMethods.put(Option.InsertDriver, new DbWorker() {public void doWork() {App.this.Data.InsertDriver(__connectionString);}});
		__dbMethods.put(Option.VehicleOutdated, new DbWorker() {public void doWork() {App.this.Data.VehicleOutdated(__connectionString);}});
		__dbMethods.put(Option.VehicleTotal, new DbWorker() {public void doWork() {App.this.Data.VehicleTotal(__connectionString);}});
		__dbMethods.put(Option.MostViagens, new DbWorker() {public void doWork() {App.this.Data.MostViagens(__connectionString);}});
		__dbMethods.put(Option.NoViagensCondutores, new DbWorker() {public void doWork() {App.this.Data.NoViagensCondutores(__connectionString);}});
		__dbMethods.put(Option.ViagensProprietario, new DbWorker() {public void doWork() {App.this.Data.ViagensProprietario(__connectionString);}});
		__dbMethods.put(Option.ViagensCondutores, new DbWorker() {public void doWork() {App.this.Data.ViagensCondutores(__connectionString);}});

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
			System.out.println("2. Adicionar um novo condutor");
			System.out.println("3. Colocar veiculo fora de serviço pela matrícula");
			System.out.println("4. Calcular horas totais, kilómetros e o custo total de viagens feitas");
			System.out.println("5. Clientes com mais viagens");
			System.out.println("6. Condutores sem viagens");
			System.out.println("7. Viagens realizadas pelos carros de um proprietário num dado ano");
			System.out.println("8. Condutor com o maior número de viagens num dado ano");
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
	
}


public class TP3
{
	public static void main(String[] args) throws SQLException,Exception
	{
		String url = "jdbc:postgresql://10.62.73.73:5432/?user=mp29&password=mp29&ssl=false";
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