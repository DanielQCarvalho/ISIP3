package javaInterface;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Scanner;

public class Data {
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
    public static void checkVeiculos(String url){
        try{
            LocalDateTime ld = LocalDateTime.now();
            Integer year = ld.getYear();

            Connection conn = DriverManager.getConnection(url);
            Statement stmt = conn.createStatement();

            String carListQuery = "select id, matricula, tipo, modelo, marca, ano, proprietario" +
                    " from veiculo" +
                    " where veiculo.ano<="+(year-5)+""+
                    " group by proprietario, ano, marca, modelo, tipo, matricula, id";
            ResultSet carList = stmt.executeQuery(carListQuery);
            //printResultsBetter(carList);

            while(carList.next()) {

                //System.out.println(carList.getString("matricula"));
                String matricula = carList.getString("matricula");

                ResultSet carDetails = getVehicleDetails(matricula, url, false); //Guarda a row do carro para fora de serviço

                if(carDetails.next()){

                    String mat;
                    int tipo;
                    String mod;
                    String marca;
                    int ano;
                    int prop;
                    int[] totals;

                    ResultSet viagemList = getViagemDetails(carDetails, url);

                    mat = carDetails.getString("matricula");
                    tipo = carDetails.getInt("tipo");
                    mod = carDetails.getString("modelo");
                    marca = carDetails.getString("marca");
                    ano = carDetails.getInt("ano");
                    prop = carDetails.getInt("proprietario");

                    if (viagemList.next()) {
                        totals = getTotalViagem(viagemList);
                    }else{
                        totals = new int[]{0, 0, 0, 0};
                    }
                    //printResults(list, query);

                    carListQuery = "delete from veiculo v where v.matricula ='"+matricula+"'";
                    stmt.executeUpdate(carListQuery); //Elimina a row do carro para fora de serviço

                    createVehicleOldTable(url);

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
                }
            }
        }catch (Exception e){
            System.out.println(e);
        }
    }

    //Retorna as seguintes caract. de um carro: id, matricula, tipo, nodelo, marca, nviagens, ano e proprietario.
    private static ResultSet getVehicleDetails(String matricula, String url, Boolean isViagemImportant){
        try{
            String[] bool;
            if(isViagemImportant) {
                bool = new String[]{"count(id) as numerodeviagens,", ", viagem", "and viagem.veiculo=id"};
            }else{
                bool = new String[]{"","",""};
            }
            Connection conn = DriverManager.getConnection(url);
            Statement stmt = conn.createStatement();
            String carQuery = "select id, matricula, tipo, modelo, marca, "+bool[0]+" ano, proprietario" +
                    " from veiculo "+bool[1]+"" +
                    " where veiculo.matricula='"+matricula+"' "+bool[2]+"" +
                    " group by proprietario, ano, marca, modelo, tipo, matricula, id"; //Carro com n de viagens
            return stmt.executeQuery(carQuery); //Guarda a row do carro
        }catch (Exception e){
            System.out.println(e);
        }
        return null;
    }

    //Retorna todas as viagens e as suas caracteristicas de um carro.
    private static ResultSet getViagemDetails(ResultSet carDetails, String url){
        try{
            Connection conn = DriverManager.getConnection(url);
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
    private static long getTimeDiff(Time inicio, Time fim){
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
    private static int[] getTotalViagem(ResultSet viagemList){
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

    private static void printResultsBetter(ResultSet dr){
        try {
            int idx = 0;
            while(dr.getMetaData().getColumnCount() != idx){
                idx++;
                System.out.print(dr.getMetaData().getColumnLabel(idx)+" ");
            }
            System.out.println();
            System.out.println("-".repeat(dr.getMetaData().getColumnCount()*8));
            while(dr.next()){
                for(var i = 1; i <= dr.getMetaData().getColumnCount(); i++){
                    System.out.print(dr.getString(i)+" ");
                }
                System.out.println();
            }
        }catch (Exception e){
            System.out.println(e);
        }
        /*
        Result must be similar like:
        ListDepartment()
        dname   		dnumber		mgrssn      mgrstartdate
        -----------------------------------------------------
        Research		5  			333445555 	1988-05-22
        Administration	4    		987654321	1995-01-01
        */
    }

    private void InsertPerson(String url) {
        ArrayList<String> personDetails = new ArrayList<>();
        String[] personDetailsNames = {"NIdentificacao", "NIF", "Nome Proprio", "Apelido", "Morada", "Numero de telefone", "Localidade", "Atributo"};
        int i = 0;
        do {
            String input = readInput(personDetailsNames[i]);
            personDetails.add(input);
            i++;
        } while (personDetails.size() < 8);
        try{
            Connection conn = DriverManager.getConnection(url);
            Statement stmt = conn.createStatement();
            String query = "insert into pessoa (id, noident, nif, nproprio, apelido, morada, telnumber, localidade, atrdisc) \n" +
                    "values(DEFAULT, '"+personDetails.get(0)+"','"+personDetails.get(1)+"','"+personDetails.get(2)+"','"+personDetails.get(3)+"','"+personDetails.get(4)+"','"+personDetails.get(5)+"','"+personDetails.get(6)+"','"+personDetails.get(7)+"')";
            stmt.executeUpdate(query);
        }catch (Exception e){
            System.out.println(e);
        }
    }

    void InsertDriver(String url)
    {
        String nif=readInput("Insira o NIF do condutor.");
        String query = "select id"+
                " from pessoa where pessoa.nif='"+nif+"'";
        try {
            Connection conn = DriverManager.getConnection(url);
            Statement stmt = conn.createStatement();

            ResultSet list = stmt.executeQuery(query);
            if(!list.next()){
                System.out.println("Essa pessoa não existe na nossa base de dados, por favor insira as seguintes informações.");
                InsertPerson(url);
            }
            query = "select atrdisc , id"+
                    " from pessoa where pessoa.nif='"+nif+"'";
            list = stmt.executeQuery(query);
            list.next();
            if(list.getObject("atrdisc") == "P"){
                System.out.println("Esta pessoa nao pode ser condutor por ser proprietario");
            }else{
                query = "insert into condutor(idpessoa, ncconducao, dtnascimento)"+
                        " values (?,?,?)";
                PreparedStatement pstmt = conn.prepareStatement(query);
                String cartadeConducao = readInput("Insira o numero da carta de conducao do condutor. (ex: cc-123456789)").toLowerCase();
                String dataDeNascimento = readInput("Insira a data de nascimento do condutor. (ex: 1970-05-05)");
                int id = list.getInt("id");
                pstmt.setInt(1, id);
                pstmt.setString(2, cartadeConducao );
                pstmt.setDate(3, Date.valueOf(dataDeNascimento));
                pstmt.executeUpdate();
                System.out.println("Condutor adicionado");
            }
        }catch (Exception e){
            System.out.println(e);
        }
    }

    static void createVehicleOldTable(String url){
        try {
            Connection conn = DriverManager.getConnection(url);
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

    void VehicleOutdated(String url)
    {
        try {
            Connection conn = DriverManager.getConnection(url);
            Statement stmt = conn.createStatement();

            String carListQuery = "select matricula,tipo,modelo,marca,ano" +
                    " from veiculo";
            ResultSet carList = stmt.executeQuery(carListQuery);

            printResultsBetter(carList);

            String input = readInput("Insira a matrícula do veiculo que quer colocar fora de serviço (ex: CC13DD)");

            ResultSet carDetails = getVehicleDetails(input, url, true); //Guarda a row do carro para fora de serviço

            String mat;
            int tipo;
            String mod;
            String marca;
            int ano;
            int prop;
            int[] totals = {0,0,0,0};

            if(carDetails.next()) {

                ResultSet viagemList = getViagemDetails(carDetails, url);

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

            carListQuery = "delete from veiculo v where v.matricula ='"+input+"'";
            stmt.executeUpdate(carListQuery); //Elimina a row do carro para fora de serviço

            createVehicleOldTable(url);

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

    void VehicleTotal(String url)
    {
        try {
            Connection conn = DriverManager.getConnection(url);
            Statement stmt = conn.createStatement();

            String cars = "select matricula, modelo, marca, ano from veiculo";
            ResultSet carsList = stmt.executeQuery(cars);
            printResultsBetter(carsList);; //Mostra a lista de carros

            String input = readInput("Insira a matricula do carro"); //Espera pelo input da matricula
            ResultSet carDetails = getVehicleDetails(input, url, true); //Busca os detalhes do carro
            int[] total = {0,0,0,0};

            if(carDetails.next()){

                ResultSet viagemList = getViagemDetails(carDetails, url); //Busca as viagens que um carro fez

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
    void MostViagens(String url)
    {
        try {
            Connection conn = DriverManager.getConnection(url);
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
    void NoViagensCondutores(String url)
    {
        try {
            Connection conn = DriverManager.getConnection(url);
            Statement stmt = conn.createStatement();

            String query = "select id,nproprio,apelido,nif\n" +
                    " from pessoa," +
                    " (select idpessoa" +
                    " from condutor" +
                    " EXCEPT" +
                    " (select idpessoa" +
                    " from condutor" +
                    " INTERSECT" +
                    " select condutor" +
                    " from viagem))as NRV" +
                    " where id=NRV.idpessoa;";
            ResultSet list = stmt.executeQuery(query);
            printResultsBetter(list);
        }catch (Exception e){
            System.out.println(e);
        }
    }

    //3b
    void ViagensProprietario(String url)
    {
        try {
            Connection conn = DriverManager.getConnection(url);
            Statement stmt = conn.createStatement();

            String input = readInput("Insira o NIF ou o nome e apelido do proprietário e o ano que deseja consultar");
            String[] inputSplit = input.split(" ");

            String query = "";
            if(inputSplit.length == 3) {
                //Procura com base do nome e apelido
                query = "select veiculo , count(dtviagem) as NumeroDeViagens\n" +
                        " from viagem, (select veiculo.id,proprietario\n" +
                        " from veiculo,(select id\n" +
                        " from pessoa\n" +
                        " where nproprio = '"+inputSplit[0]+"' and apelido = '"+inputSplit[1]+"') as prop\n" +
                        " where prop.id = proprietario) as v\n" +
                        " where dtviagem::text LIKE '"+inputSplit[2]+"%' and veiculo = v.id\n" +
                        " group by veiculo;";
            }else{
                //Procura com base no NIF
                query = "select veiculo, count(dtviagem) as NumeroDeViagens"+
                " from viagem, (select veiculo.id, proprietario"+
                " from veiculo,(select id"+
                " from pessoa"+
                " where nif = '"+inputSplit[0]+"') as prop"+
                " where proprietario=prop.id) as v"+
                " where dtviagem::text LIKE '"+inputSplit[1]+"%' and veiculo = v.id"+
                " group by veiculo;";
            }

            ResultSet list = stmt.executeQuery(query);
            printResultsBetter(list);
        }catch (Exception e){
            System.out.println(e);
        }
    }

    //3c
    void ViagensCondutores(String url)
    {
        try {
            Connection conn = DriverManager.getConnection(url);
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