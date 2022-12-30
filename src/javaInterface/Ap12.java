package javaInterface; /**
ISEL - DEETC
Introdução a Sistemas de Informação
MP,ND, 2014-2022
*/

import org.postgresql.jdbc.SslMode;

import java.sql.*;
import java.sql.Date;
import java.time.Duration;
import java.time.LocalTime;
import java.time.temporal.Temporal;
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
	
	private App()
	{
		__dbMethods = new HashMap<Option,DbWorker>();
		__dbMethods.put(Option.InsertDriver, new DbWorker() {public void doWork() {App.this.InsertDriver();}});
		__dbMethods.put(Option.VehicleOutdated, new DbWorker() {public void doWork() {App.this.VehicleOutdated();}});
		__dbMethods.put(Option.VehicleTotal, new DbWorker() {public void doWork() {App.this.VehicleTotal();}});
		__dbMethods.put(Option.MostViagens, new DbWorker() {public void doWork() {App.this.MostViagens();}});
		__dbMethods.put(Option.NoViagensCondutores, new DbWorker() {public void doWork() {App.this.NoViagensCondutores();}});
		__dbMethods.put(Option.ViagensProprietario, new DbWorker() {public void doWork() {App.this.ViagensProprietario();}});
		__dbMethods.put(Option.ViagensCondutores, new DbWorker() {public void doWork() {App.this.ViagensCondutores();}});

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

	private static double distance(double lat1, double lon1, double lat2, double lon2, String unit) {
		if ((lat1 == lat2) && (lon1 == lon2)) {
			return 0;
		}
		else {
			double theta = lon1 - lon2;
			double dist = Math.sin(Math.toRadians(lat1)) * Math.sin(Math.toRadians(lat2)) + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) * Math.cos(Math.toRadians(theta));
			dist = Math.acos(dist);
			dist = Math.toDegrees(dist);
			dist = dist * 60 * 1.1515;
			if (unit.equals("K")) {
				dist = dist * 1.609344;
			} else if (unit.equals("N")) {
				dist = dist * 0.8684;
			}
			return (dist);
		}
	}

	//Verifica a quantidade de veiculos de um proprietario dado uma String.
	private void checkVeiculos(String input){
		try{
			String[] inputSplit = input.split(" ");
			String query = "select (select pes.id\n" +
					"from pessoa pes\n" +
					"where pes.nproprio = "+inputSplit[0]+" AND pes.apelido = "+inputSplit[1]+"), count(veiculo.id)\n" +
					"from proprietario p, veiculo\n" +
					"where p.idpessoa = veiculo.proprietario\n" +
					"group by p.idpessoa";
			for(var i = 0; i < query.length(); i++){
				System.out.println(inputSplit[i]);
			}
		}catch (Exception e){
			System.out.println(e);
		}
	}

	//Retorna as seguintes caract. de um carro: id, matricula, tipo, nodelo, marca, nviagens, ano e proprietario.
	private ResultSet getVehicleDetails(String matricula){
		try{
			Connection conn = DriverManager.getConnection(__connectionString);
			Statement stmt = conn.createStatement();
			String carQuery = "select id, matricula, tipo, modelo, marca, count(id) as numerodeviagens, ano, proprietario" +
					" from veiculo, viagem" +
					" where veiculo.matricula='"+matricula+"' and viagem.veiculo=id" +
					" group by proprietario, ano, marca, modelo, tipo, matricula, id"; //Carro com n de viagens
			return stmt.executeQuery(carQuery); //Guarda a row do carro
		}catch (Exception e){
			System.out.println(e);
		}
		return null;
	}

	//Retorna todas as viagens e as suas caracteristicas de um carro.
	private ResultSet getViagemDetails(ResultSet carDetails){
		try{
			Connection conn = DriverManager.getConnection(__connectionString);
			Statement stmt = conn.createStatement();
			String viagensQuery = "select hinicio, hfim, valfinal, latinicio, longinicio, latfim, longfim"+
					" from viagem where viagem.veiculo='"+carDetails.getInt("id")+"'";
			return stmt.executeQuery(viagensQuery);
		}catch (Exception e){
			System.out.println(e);
		}
		return null;
	}

	//Retorna os minutos entre dois objetos Time, tendo em conta a mudança de dia entre o inicio e o fim.
	private long getTimeDiff(Time inicio, Time fim){
		Time time = new Time(24);
		int diff;
		//Achamos irrealista não considerar situações da hora de inicio ser maior que a hora de chegada, logo isto trata desta mesma situação.
		//Apesar de no enunciado anterior e na DB existir uma restrição de integridade onde a hora de inicio é sempre menor que a hora de chegada,
		//incluimos a possibilidade de isto não acontecer.
		if(fim.getTime() - inicio.getTime() < 0){
			diff = (time.toLocalTime().minusMinutes(fim.toLocalTime().getMinute()).plusMinutes(inicio.toLocalTime().getMinute())).getMinute();
		} else {
			diff = (fim.toLocalTime().minusMinutes(inicio.toLocalTime().getMinute())).getMinute();
		}
		return diff;
	}

	//Retorna o total de horas, custo, distancia e o numero de viagens.
	private int[] getTotalViagem(ResultSet viagemList){
		try{
			int hoursTotal = 0;
			int costTotal = 0;
			int kmTotal = 0;
			int viagemTotal = 0;
			do {
				hoursTotal += getTimeDiff(viagemList.getTime("hinicio"), viagemList.getTime("hfim"));
				costTotal += viagemList.getDouble("valfinal");
				kmTotal += distance(viagemList.getDouble("latinicio"),
						viagemList.getDouble("longinicio"),
						viagemList.getDouble("latfim"),
						viagemList.getDouble("longfim"),
						"km");
				viagemTotal += 1;
			} while (viagemList.next());
			return new int[]{hoursTotal, costTotal, kmTotal, viagemTotal};
		}catch (Exception e){
			System.out.println(e);
		}
		return null;
	}

	//Recebe uma String para mostrar na consola, e espera por um input do utilizador para retornar.
	private String readInput(String comment){
		Scanner sc = new Scanner(System.in);
		String input;
		do{
			System.out.println(comment);
			input = sc.nextLine();
		}while(input == null);
		return input;
	}

	/*
	private void printResults(ResultSet dr, String query) //Print na consola uma view de um dado ResultSet, usando a query de modo a extrair o nome das colunas.
	{
		String[] columnNames = query.split(" ");
		List<String> listOfStrings = new ArrayList<>(Arrays.asList(columnNames));
		listOfStrings.removeIf( b -> Objects.equals(b, ",") ||
				listOfStrings.indexOf(b) == 0 || listOfStrings.indexOf(b) >= listOfStrings.indexOf("from"));

		System.out.println(listOfStrings);

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

		Result must be similar like:
		ListDepartment()
		dname   		dnumber		mgrssn      mgrstartdate            
		-----------------------------------------------------
		Research		5  			333445555 	1988-05-22            
		Administration	4    		987654321	1995-01-01
	}
	*/


	private void printResultsBetter(ResultSet dr){
		try {
			int idx = 1;
			while(dr.getMetaData().getColumnCount() != idx){
				System.out.print(dr.getMetaData().getColumnLabel(idx)+" ");
				idx++;
			}
			System.out.println();
			System.out.println("-".repeat(dr.getMetaData().getColumnCount()*8));
			idx = 1;
			while(dr.next()){
				for(var i = 1; i < dr.getMetaData().getColumnCount(); i++){
					System.out.print(dr.getString(i)+" ");
				}
				System.out.println();
				idx++;
			}
		}catch (Exception e){
			System.out.println(e);
		}
	}

	private void InsertPerson() {
		ArrayList<String> personDetails = new ArrayList<>();
		String[] personDetailsNames = {"NIdentificacao", "NIF", "Nome Proprio", "Apelido", "Morada", "Numero de telefone", "Localidade", "Atributo"};
		int i = 0;
		do {
			String input = readInput(personDetailsNames[i]);
			personDetails.add(input);
			i++;
		} while (personDetails.size() < 8);
		personDetails.forEach(System.out::println);
		try{
			Connection conn = DriverManager.getConnection(__connectionString);
			Statement stmt = conn.createStatement();
			String query = "insert into pessoa (id, noident, nif, nproprio, apelido, morada, telnumber, localidade, atrdisc) \n" +
					"values(DEFAULT, '"+personDetails.get(0)+"','"+personDetails.get(1)+"','"+personDetails.get(2)+"','"+personDetails.get(3)+"','"+personDetails.get(4)+"','"+personDetails.get(5)+"','"+personDetails.get(6)+"','"+personDetails.get(7)+"')";
			stmt.executeQuery(query);
		}catch (Exception e){
			System.out.println(e);
		}
	}

	private void InsertDriver()
	{
		String nif=readInput("Insira o NIF do condutor.");
		String query = "select id"+
				" from pessoa where pessoa.nif='"+nif+"'";
		try {
			Connection conn = DriverManager.getConnection(__connectionString);
			Statement stmt = conn.createStatement();

			ResultSet list = stmt.executeQuery(query);
			if(!list.next()){
				System.out.println("Essa pessoa não existe na nossa base de dados, por favor insira as seguintes informações.");
				InsertPerson();
			}
			query = "select atrdisc , id"+
					" from pessoa where pessoa.nif='"+nif+"'";
			list = stmt.executeQuery(query);
			list.next();
			System.out.println(list.getObject("atrdisc"));
			if(list.getObject("atrdisc") == "P"){
				System.out.println("Esta pessoa nao pode ser condutor por ser proprietario");
			}else{
				query = "insert into condutor(idpessoa, ncconducao, dtnascimento)"+
						" values (?,?,?)";
				PreparedStatement pstmt = conn.prepareStatement(query);
				{
					String cartadeConducao = readInput("Insira o numero da carta de conducao do condutor. (ex: cc-123456789)").toLowerCase();
					String dataDeNascimento = readInput("Insira a data de nascimento do condutor. (ex: 1970-05-05)");
					int id = list.getInt("id");
					pstmt.setInt(1, id);
					pstmt.setString(2, cartadeConducao );
					pstmt.setDate(3, Date.valueOf(dataDeNascimento));
					pstmt.executeUpdate();
				}
				System.out.println("Condutor adicionado");
			}
		}catch (Exception e){
			System.out.println(e);
		}
	}

	void createVehicleOldTable(){
		try {
			Connection conn = DriverManager.getConnection(__connectionString);
			Statement stmt = conn.createStatement();
			String query = "create table  if not exists VEICULO_OLD(" +
					" id serial NOT NULL PRIMARY KEY," +
					" matricula varchar(10) UNIQUE CHECK ( matricula ~ '^([A-Z]{2}[0-9]{2}[A-Z]{2})$' OR matricula ~'^([0-9]{2}[A-Z]{2}[0-9]{2})$')," +
					" tipo integer," +
					" modelo varchar(10)," +
					" marca varchar(10)," +
					" kmpercorridos integer,"+
					" nviagensrealizadas integer,"+
					" ano integer," +
					" proprietario integer CHECK ( NOT NULL)," +
					" FOREIGN KEY (tipo) REFERENCES TIPOVEICULO(tipo)," +
					" FOREIGN KEY (proprietario) REFERENCES PROPRIETARIO(idpessoa)" +
					" ON DELETE CASCADE);";
			stmt.executeQuery(query);
		}catch (Exception e){
			System.out.println(e);
		}
	}

	private void VehicleOutdated()
	{
		try {
			Connection conn = DriverManager.getConnection(__connectionString);

			String input = readInput("Insira a matrícula do veiculo que quer colocar fora de serviço (ex: CC13DD)");

			ResultSet carDetails = getVehicleDetails(input); //Guarda a row do carro para fora de serviço

			String mat;
			int tipo;
			String mod;
			String marca;
			int ano;
			int prop;
			int[] totals = {0,0,0,0};

			if(carDetails.next()) {

				ResultSet viagemList = getViagemDetails(carDetails);

				mat = carDetails.getString("matricula");
				tipo = carDetails.getInt("tipo");
				mod = carDetails.getString("modelo");
				marca = carDetails.getString("marca");
				ano = carDetails.getInt("ano");
				prop = carDetails.getInt("proprietario");

				if(viagemList.next()){
					totals = getTotalViagem(viagemList);
				}
			}else{
				throw new Exception("A dada matrícula não existe na base de dados");
			}

			//printResults(list, query);

			//query = "delete from veiculo v where v.matricula ='"+input+"'";
			//stmt.executeQuery(query); //Elimina a row do carro para fora de serviço

			createVehicleOldTable();

			String carQuery = "insert into veiculo_old(matricula, tipo, modelo, marca, kmpercorridos, nviagensrealizadas, ano, proprietario)" +
					" values (?,?,?,?,?,?,?,?)";
			PreparedStatement pstmt = conn.prepareStatement(carQuery);
			pstmt.setString(1, mat);
			pstmt.setInt(2, tipo);
			pstmt.setString(3, mod);
			pstmt.setString(4, marca);
			pstmt.setInt(5, totals[2]);
			pstmt.setInt(6, totals[3]);
			pstmt.setInt(7, ano);
			pstmt.setInt(8, prop);
			pstmt.executeUpdate();

		}catch (Exception e){
			System.out.println(e);
		}
	}

	private void VehicleTotal()
	{
		try {
			Connection conn = DriverManager.getConnection(__connectionString);
			Statement stmt = conn.createStatement();

			String cars = "select matricula, modelo, marca, ano from veiculo";
			ResultSet carsList = stmt.executeQuery(cars);
			printResultsBetter(carsList);; //Mostra a lista de carros

			String input = readInput("Insira a matricula do carro"); //Espera pelo input da matricula
			ResultSet carDetails = getVehicleDetails(input); //Busca os detalhes do carro
			int[] total = {0,0,0,0};

			if(carDetails.next()){

				ResultSet viagemList = getViagemDetails(carDetails); //Busca as viagens que um carro fez

				if(viagemList.next()) {
					total = getTotalViagem(viagemList);
				}
				System.out.println("Horas totais "+total[0]/60+"h:"+total[0]%60+"m");
				System.out.println("Kilómetros totais "+total[2]);
				System.out.println("Custo total "+total[1]);
			}else{
				throw new Exception("O carro não existe na base de dados");
			}
		}catch (Exception e){
			System.out.println(e);
		}
	}

	//2c
	private void MostViagens()
	{
		try {
			Connection conn = DriverManager.getConnection(__connectionString);
			Statement stmt = conn.createStatement();

			String input = readInput("Insira o ano que deseja consultar");

			String query = "select id,nproprio, apelido, viagensOn"+input+"\n" +
					"from (select id, nproprio, apelido, count(dtviagem) as viagensOn"+input+"\n" +
					"        from pessoa, (select periodoactivo.condutor,dtviagem\n" +
					"            from periodoactivo, viagem\n" +
					"            where viagem.condutor = periodoactivo.condutor AND dtviagem::text LIKE '"+input+"%') as date\n" +
					"        where id=date.condutor\n" +
					"    group by id) viagens\n" +
					"where viagens.viagensOn"+input+" = (select max(viagensOn"+input+") from (select id, nproprio, apelido, count(dtviagem) as viagensOn"+input+"\n" +
					"        from pessoa, (select periodoactivo.condutor, dtviagem\n" +
					"            from periodoactivo, viagem\n" +
					"            where viagem.condutor = periodoactivo.condutor AND dtviagem::text LIKE '"+input+"%') as date\n" +
					"        where id = date.condutor\n" +
					"    group by id) viagens)";
			ResultSet list = stmt.executeQuery(query);
			printResultsBetter(list);
		}catch (Exception e){
			System.out.println(e);
		}
	}

	//2d
	private void NoViagensCondutores()
	{
		try {
			Connection conn = DriverManager.getConnection(__connectionString);
			Statement stmt = conn.createStatement();

			String query = "";
			ResultSet list = stmt.executeQuery(query);
			printResultsBetter(list);
		}catch (Exception e){
			System.out.println(e);
		}
	}

	//3b
	private void ViagensProprietario()
	{
		try {
			Connection conn = DriverManager.getConnection(__connectionString);
			Statement stmt = conn.createStatement();

			String input = readInput("Insira o NIF ou o nome e apelido do proprietário e o ano que deseja consultar");
			String[] inputSplit = input.split(" ");

			String query = "";
			if(inputSplit.length == 3) {
				query = "select veiculo , count(dtviagem) as NumeroDeViagens\n" +
						"from viagem, (select veiculo.id,proprietario\n" +
						"        from veiculo,(select id\n" +
						"            from pessoa\n" +
						"            where nif = '123456789123') as prop\n" +
						"        where prop.id = proprietario) as v\n" +
						"where dtviagem::text LIKE '"+inputSplit[2]+"%' and veiculo = v.proprietario\n" +
						"group by veiculo;"; //Procura com base do nome e apelido
			}else{
				query = "select veiculo, count(dtviagem) as NumeroDeViagens\n" +
						"from viagem, (select veiculo.id, proprietario\n" +
						"        from veiculo,(select id\n" +
						"            from pessoa\n" +
						"            where nif = '"+inputSplit[0]+"') as prop\n" +
						"        where prop.id = proprietario) as v\n" +
						"where dtviagem::text LIKE '"+inputSplit[1]+"%' and veiculo = v.proprietario\n" +
						"group by veiculo;"; //Procura com base no NIF
			}

			ResultSet list = stmt.executeQuery(query);
			printResultsBetter(list);
		}catch (Exception e){
			System.out.println(e);
		}
	}

	//3c
	private void ViagensCondutores()
	{
		try {
			Connection conn = DriverManager.getConnection(__connectionString);
			Statement stmt = conn.createStatement();

			String input = readInput("Insira o ano que deseja consultar");

			String query = "select id, noident, nproprio, apelido, morada\n" +
					"    from pessoa,\n" +
					"        (select condutor, total\n" +
					"        from (select condutor, sum(valfinal) as total\n" +
					"            from viagem\n" +
					"                where dtviagem::text LIKE '"+input+"%' and condutor = viagem.condutor\n" +
					"            group by condutor) as totalmoney\n" +
					"        where total=(select max(total) from (select condutor, sum(valfinal) as total\n" +
					"            from viagem\n" +
					"                where dtviagem::text LIKE '"+input+"%' and condutor = viagem.condutor\n" +
					"            group by condutor) as totalmoney )) as maxTotal\n" +
					"where pessoa.id = maxTotal.condutor\n" +
					";";
			ResultSet list = stmt.executeQuery(query);

			printResultsBetter(list);
		}catch (Exception e){
			System.out.println(e);
		}
	}
	
}

public class Ap12
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