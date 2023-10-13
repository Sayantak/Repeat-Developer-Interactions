package comments_Eclipse;

/**
 * There are 3 tables :
 * 1. categoryTracker - It is used to keep track of the overall comments made by a developer and accordingly his category
 * 2. vertices - It is used to keep track of which developer and how many comments (weight attribute) made by the developer in a particular issue_id
 * 3. Edges_OS - It is used to build the network and keep record of each interaction
 *  All Tables are created in main method
 */

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.*;
import java.util.Scanner;


public class Interaction_Network_Eclipse {
    String issue_id="", dev_id="";
    static int slno;
    static int slno1;
    static int n;
    Integer[] boundary= new Integer[n-1]; // boundary[] will store the boundary values for Categorization

    /**----------------------------------------------------------------------------------------------------------------------------------------------------------------**/

    // This function calculates the boundary values of no_of_comments to categorize the developers
    public void categoryDecider() throws IOException {
        String line;
        int j;

        String[] no_of_comments = new String[4078];  //Total developers are 419(top 10%)
        int i = 0;

        BufferedReader br = new BufferedReader(new FileReader("dev_comments_4078.csv"));
        while ((line = br.readLine()) != null) {
            String[] cols = line.split(",");
            no_of_comments[i] = cols[1];
            i++;
        }

        int boundary1=no_of_comments.length/n;

        for (j=0;j<n-1;j++)
        {
            boundary[j]=Integer.parseInt(no_of_comments[boundary1*(j+1)]);  // Storing the boundary values of Category
        }

        for (j=0;j<n-1;j++)
        {
            System.out.println("Boundary-"+(j+1)+" = "+boundary[n-2-j]);
        }
    }


    /**----------------------------------------------------------------------------------------------------------------------------------------------------------------**/

    // Read issue_id and developer_id from the csv file row by row
    public void read_CSVFile() throws IOException, ClassNotFoundException, SQLException {
        String line;
        int row_number=1;

        BufferedReader br = new BufferedReader(new FileReader("eclipse_final.csv"));
        while ((line = br.readLine()) != null) {
            String[] columns = line.split(",");
            issue_id = columns[0];
            dev_id= columns[1];
            System.out.println("Current Issue ID= " + issue_id);
            System.out.println("Current Row number = "+ row_number);
            System.out.println("\n");
            row_number++;
            VerticesBuilder(issue_id,dev_id);

        }
    }

    /**----------------------------------------------------------------------------------------------------------------------------------------------------------------**/

    // Check if the developer is already present in the Table or not. If not then insert the developer in the Table
    public void VerticesBuilder(String issue_id, String dev_id) throws ClassNotFoundException, SQLException, IOException {
        int count=0;
        int fc=1; // fc=first_comment
        String category="1"; //stores the category of the current developer
        String prevCategory="1";
        //String issue=issue_id;
        //String dev=dev_id;

        Class.forName("com.mysql.cj.jdbc.Driver");
        Connection con=DriverManager.getConnection("jdbc:mysql://localhost:3306/eclipse", "root", "Sayantak1");

        // This query checks whether developer already commented previously or not.
        String query="SELECT comment_count, category FROM categoryTracker WHERE dev_id='"+dev_id+"';";

        Statement st=con.createStatement();
        Statement st1=con.createStatement();
        Statement st2=con.createStatement();
        ResultSet rs=st.executeQuery(query);

        if (!rs.next())
        {
            slno++;
            String q1= "INSERT INTO categoryTracker(slno,dev_id,comment_count,category) VALUES ("+slno+",'"+dev_id+"',"+fc+",'1');";
            String query1= "insert into vertices(dev_id, issue_id, no_of_comments) values('"+dev_id+"','"+issue_id+"', 1);";
            st.executeUpdate(q1);
            st1.executeUpdate(query1);
        }
        else {

            //----------------------------------------------------------------------------------------------------------------------------
            int comment_count= rs.getInt("comment_count");
            prevCategory=rs.getString("category");
            comment_count++;  // increase comment count of CategoryTracker table by 1

            // Checking whether the developer is present in vertices table for current issue_id
            String q6="Select * from vertices where dev_id='"+dev_id+"' and issue_id='"+issue_id+"';";
            ResultSet rs1= st2.executeQuery(q6);
            if (!rs1.next())
            {
                String query2= "insert into vertices(dev_id, issue_id, no_of_comments) values('"+dev_id+"','"+issue_id+"', 1);";
                st.executeUpdate(query2);
            }
            else
            {
                int noc=rs1.getInt("no_of_comments");  //noc=no_of_comments
                noc++; // Increase the no of comments in Vertices table by 1
                String query3="update vertices set no_of_comments ="+noc+" where dev_id='"+dev_id+"' and issue_id='"+issue_id+"';";
                st.executeUpdate(query3);
            }

            //----------------------------------------------------------------------------------------------------------------------------


            // Updation of comment_count and category of dev_id in categoryTracker table
            if(comment_count<boundary[2])  // Replace b1 with the C1 boundary value
            {
                category="1";
                String q2="update categoryTracker set comment_count ="+comment_count+", category = '1' where dev_id ='"+dev_id+"';";
                st.executeUpdate(q2);
            }
            else if(comment_count>=boundary[2] &&  comment_count<boundary[1])
            {
                category="2";
                String q3= "update categoryTracker set comment_count ="+comment_count+", category = '2' where dev_id ='"+dev_id+"';";
                st.executeUpdate(q3);
            }
            else if(comment_count>=boundary[1] &&  comment_count<boundary[0])
            {
                category="3";
                String q4= "update categoryTracker set comment_count ="+comment_count+", category = '3' where dev_id ='"+dev_id+"';";
                st.executeUpdate(q4);
            }
            else if(comment_count>=boundary[0])
            {
                category="4";
                String q5= "update categoryTracker set comment_count ="+comment_count+", category = '4' where dev_id ='"+dev_id+"';";
                st.executeUpdate(q5);
            }

        }

        con.close();
        st.close();
        rs.close();

        System.out.println("Category of dev_id= "+dev_id+" is "+prevCategory);
        edgesBuilder(issue_id,dev_id,prevCategory);
    }

    /**----------------------------------------------------------------------------------------------------------------------------------------------------------------**/

    /*The network connections are built here.
      If no edge exists it is added and interaction type is recorded.
      If it exists the the weight is incremented by 1 and interaction type is updated.*/

    public void edgesBuilder(String issue_id, String dev_id, String category) throws ClassNotFoundException, SQLException, IOException {

        int weight = 0, new_weight = 0,update_weight=0,q=0;
        String cat1=category;
        String condition;
        String interaction_type;
        Class.forName("com.mysql.cj.jdbc.Driver");
        Connection con = DriverManager.getConnection("jdbc:mysql://localhost:3306/eclipse", "root", "Sayantak1");
        Statement stmt1 = con.createStatement();
        Statement stmt2 = con.createStatement();
        Statement stmt3 = con.createStatement();
        Statement stmt4 = con.createStatement();
        String query1 = "Select dev_id from vertices where issue_id= '" + issue_id +"' and dev_id <> '" + dev_id + "';"; // Find out all other developers who have commented in this issue_id
        ResultSet rs1 = stmt1.executeQuery(query1);

        while (rs1.next()) {

            String dev2 = rs1.getString("dev_id");
            String query2 = "Select * from Edges_OS where dev1 ='" + dev_id + "' and dev2='" + dev2 + "';";
            String reverse_query2 = "Select * from Edges_OS where dev1 ='" + dev2 + "' and dev2='" + dev_id + "';";

            ResultSet rs2 = stmt2.executeQuery(query2);
            ResultSet rq2 = stmt3.executeQuery(reverse_query2);  // rq2= reverse_query2

            while (rs2.next()) {
                weight = rs2.getInt("weight");
            }
            while (rq2.next()) {
                new_weight = rq2.getInt("weight");
            }

            String cat2=checkCategoryOfOtherDev(dev2);  // cat2 is the category of dev2

            if (weight == 0 && new_weight == 0) //When they do not have an edge between them. In this case we insert an entry in the edges_OS table.
            {
                slno1++;
                condition="new";
                String query3="Insert into Edges_OS (slno, dev1, dev2, interaction_type, weight) values ("+slno1+", '"+dev_id+"','"+dev2+"', '"+cat1+cat2+"', 1);";
                stmt4.executeUpdate(query3);
                interaction_type=cat1+cat2;
                System.out.println("Interaction type = "+ interaction_type);
                pqnBuilder(issue_id, interaction_type, condition);
            }
            else if(weight != 0 && new_weight == 0) // Edge exists between dev_id and dev2
            {
                q++;
                condition="repeat";
                update_weight=weight+1;
                String query3="Update Edges_OS set interaction_type ='"+cat1+cat2+"', weight="+update_weight+" where dev1='"+dev_id+"' and dev2= '"+dev2+"';";
                stmt4.executeUpdate(query3);
                interaction_type=cat1+cat2;
                System.out.println("Interaction type = "+ interaction_type);
                pqnBuilder(issue_id, interaction_type, condition);
                update_repeat_edges(issue_id,dev2,dev_id,q);
            }

            else if(weight==0 && new_weight!=0) // Edge exists between dev2 and dev_id
            {
                q++;
                condition="repeat";
                update_weight=new_weight+1;
                String query3="Update Edges_OS set interaction_type ='"+cat2+cat1+"', weight="+update_weight+" where dev1='"+dev2+"' and dev2= '"+dev_id+"';";
                stmt4.executeUpdate(query3);
                interaction_type=cat2+cat1;
                System.out.println("Interaction type = "+ interaction_type);
                pqnBuilder(issue_id, interaction_type, condition);
                update_repeat_edges(issue_id,dev2,dev_id,q);
            }

            rs2.close();
            rq2.close();
        }

        con.close();
        stmt1.close();
        stmt2.close();
        stmt3.close();
        stmt4.close();
        rs1.close();

        //update_repeat_edges(issue_id,q);
    }

    /**----------------------------------------------------------------------------------------------------------------------------------------------------------------**/

    void update_repeat_edges(String issue_id,String dev2, String dev_id, int q) throws ClassNotFoundException, SQLException
    {
        Class.forName("com.mysql.cj.jdbc.Driver");
        Connection con = DriverManager.getConnection("jdbc:mysql://localhost:3306/eclipse", "root", "Sayantak1");
        Statement stmt = con.createStatement();
        Statement stmt1 = con.createStatement();
        Statement stmt2 = con.createStatement();
        String query2 = "Select * from repeat_edges where issue_id='"+issue_id+"' and dev1 ='" + dev_id + "' and dev2='" + dev2 + "';";
        String reverse_query2 = "Select * from repeat_edges  where issue_id='"+issue_id+"' and dev1 ='" + dev2 + "' and dev2='" + dev_id + "';";
        ResultSet rs2 = stmt.executeQuery(query2);
        ResultSet rq2 = stmt2.executeQuery(reverse_query2);

        if(!rs2.next() && !rq2.next())
        {
            String query1="Insert into repeat_edges (issue_id,dev1,dev2,repeat_edges) values('"+issue_id+"', '"+dev_id+"', '"+dev2+"', "+1+");";
            stmt1.executeUpdate(query1);
        }

        con.close();
        stmt.close();
        stmt1.close();
        stmt2.close();
        rs2.close();
        rq2.close();
    }


    /**----------------------------------------------------------------------------------------------------------------------------------------------------------------**/

    //This function fetches the present Category of other developer
    public String checkCategoryOfOtherDev(String dev2) throws  SQLException , ClassNotFoundException{

        String cat2="";
        Class.forName("com.mysql.cj.jdbc.Driver");
        Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/eclipse", "root", "Sayantak1");
        Statement st2 = conn.createStatement();

        String q2="select category from categoryTracker where dev_id ='"+dev2+"';";
        ResultSet rs2=st2.executeQuery(q2);

        while (rs2.next()) {
            cat2 = rs2.getString("category");
        }
        st2.close();
        conn.close();

        return cat2;

    }

    /*------------------------------------------------------------------------------------------------------------------------------------------------------------*/

    //This function keeps record of each type of interaction in each issue_ID
    public void pqnBuilder(String issue_id, String interaction_type, String condition) throws ClassNotFoundException, SQLException, IOException
    {
        Class.forName("com.mysql.cj.jdbc.Driver");
        Connection connection = DriverManager.getConnection("jdbc:mysql://localhost:3306/eclipse", "root", "Sayantak1");

        Statement st1 = connection.createStatement();
        Statement st2 = connection.createStatement();

        if(condition.equals("new")) {
            String q1 = "Select * from pqnBuilder where issue_id ='" + issue_id + "';";
            ResultSet rs1 = st1.executeQuery(q1);

            if (!rs1.next()) {
                if (interaction_type.equals("11")) {
                    String q2 = "INSERT INTO pqnBuilder(issue_id,c1_c1_fresh,c1_c2_fresh,c1_c3_fresh,c1_c4_fresh,c2_c2_fresh,c2_c3_fresh,c2_c4_fresh,c3_c3_fresh,c3_c4_fresh,c4_c4_fresh,"+
                            "c1_c1_repeat,c1_c2_repeat,c1_c3_repeat,c1_c4_repeat,c2_c2_repeat,c2_c3_repeat,"+
                            "c2_c4_repeat,c3_c3_repeat,c3_c4_repeat,c4_c4_repeat) VALUES ('" + issue_id + "',1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0);";
                    st2.executeUpdate(q2);
                } else if (interaction_type.equals("12") || interaction_type.equals("21")) {
                    String q2 = "INSERT INTO pqnBuilder(issue_id,c1_c1_fresh,c1_c2_fresh,c1_c3_fresh,c1_c4_fresh,c2_c2_fresh,c2_c3_fresh,c2_c4_fresh,c3_c3_fresh,c3_c4_fresh,c4_c4_fresh,"+
                            "c1_c1_repeat,c1_c2_repeat,c1_c3_repeat,c1_c4_repeat,c2_c2_repeat,c2_c3_repeat,"+
                            "c2_c4_repeat,c3_c3_repeat,c3_c4_repeat,c4_c4_repeat) VALUES ('" + issue_id + "',0,1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0);";
                    st2.executeUpdate(q2);
                } else if (interaction_type.equals("13") || interaction_type.equals("31")) {
                    String q2 = "INSERT INTO pqnBuilder(issue_id,c1_c1_fresh,c1_c2_fresh,c1_c3_fresh,c1_c4_fresh,c2_c2_fresh,c2_c3_fresh,c2_c4_fresh,c3_c3_fresh,c3_c4_fresh,c4_c4_fresh,"+
                            "c1_c1_repeat,c1_c2_repeat,c1_c3_repeat,c1_c4_repeat,c2_c2_repeat,c2_c3_repeat,"+
                            "c2_c4_repeat,c3_c3_repeat,c3_c4_repeat,c4_c4_repeat) VALUES ('" + issue_id + "',0,0,1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0);";
                    st2.executeUpdate(q2);
                } else if (interaction_type.equals("14") || interaction_type.equals("41")) {
                    String q2 = "INSERT INTO pqnBuilder(issue_id,c1_c1_fresh,c1_c2_fresh,c1_c3_fresh,c1_c4_fresh,c2_c2_fresh,c2_c3_fresh,c2_c4_fresh,c3_c3_fresh,c3_c4_fresh,c4_c4_fresh,"+
                            "c1_c1_repeat,c1_c2_repeat,c1_c3_repeat,c1_c4_repeat,c2_c2_repeat,c2_c3_repeat,"+
                            "c2_c4_repeat,c3_c3_repeat,c3_c4_repeat,c4_c4_repeat) VALUES ('" + issue_id + "',0,0,0,1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0);";
                    st2.executeUpdate(q2);
                } else if (interaction_type.equals("22")) {
                    String q2 = "INSERT INTO pqnBuilder(issue_id,c1_c1_fresh,c1_c2_fresh,c1_c3_fresh,c1_c4_fresh,c2_c2_fresh,c2_c3_fresh,c2_c4_fresh,c3_c3_fresh,c3_c4_fresh,c4_c4_fresh,"+
                            "c1_c1_repeat,c1_c2_repeat,c1_c3_repeat,c1_c4_repeat,c2_c2_repeat,c2_c3_repeat,"+
                            "c2_c4_repeat,c3_c3_repeat,c3_c4_repeat,c4_c4_repeat) VALUES ('" + issue_id + "',0,0,0,0,1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0);";
                    st2.executeUpdate(q2);
                } else if (interaction_type.equals("23") || interaction_type.equals("32")) {
                    String q2 = "INSERT INTO pqnBuilder(issue_id,c1_c1_fresh,c1_c2_fresh,c1_c3_fresh,c1_c4_fresh,c2_c2_fresh,c2_c3_fresh,c2_c4_fresh,c3_c3_fresh,c3_c4_fresh,c4_c4_fresh,"+
                            "c1_c1_repeat,c1_c2_repeat,c1_c3_repeat,c1_c4_repeat,c2_c2_repeat,c2_c3_repeat,"+
                            "c2_c4_repeat,c3_c3_repeat,c3_c4_repeat,c4_c4_repeat) VALUES ('" + issue_id + "',0,0,0,0,0,1,0,0,0,0,0,0,0,0,0,0,0,0,0,0);";
                    st2.executeUpdate(q2);
                } else if (interaction_type.equals("24") || interaction_type.equals("42")) {
                    String q2 = "INSERT INTO pqnBuilder(issue_id,c1_c1_fresh,c1_c2_fresh,c1_c3_fresh,c1_c4_fresh,c2_c2_fresh,c2_c3_fresh,c2_c4_fresh,c3_c3_fresh,c3_c4_fresh,c4_c4_fresh,"+
                            "c1_c1_repeat,c1_c2_repeat,c1_c3_repeat,c1_c4_repeat,c2_c2_repeat,c2_c3_repeat,"+
                            "c2_c4_repeat,c3_c3_repeat,c3_c4_repeat,c4_c4_repeat) VALUES ('" + issue_id + "',0,0,0,0,0,0,1,0,0,0,0,0,0,0,0,0,0,0,0,0);";
                    st2.executeUpdate(q2);
                } else if (interaction_type.equals("33")) {
                    String q2 = "INSERT INTO pqnBuilder(issue_id,c1_c1_fresh,c1_c2_fresh,c1_c3_fresh,c1_c4_fresh,c2_c2_fresh,c2_c3_fresh,c2_c4_fresh,c3_c3_fresh,c3_c4_fresh,c4_c4_fresh,"+
                            "c1_c1_repeat,c1_c2_repeat,c1_c3_repeat,c1_c4_repeat,c2_c2_repeat,c2_c3_repeat,"+
                            "c2_c4_repeat,c3_c3_repeat,c3_c4_repeat,c4_c4_repeat) VALUES ('" + issue_id + "',0,0,0,0,0,0,0,1,0,0,0,0,0,0,0,0,0,0,0,0);";
                    st2.executeUpdate(q2);
                } else if (interaction_type.equals("34") || interaction_type.equals("43")) {
                    String q2 = "INSERT INTO pqnBuilder(issue_id,c1_c1_fresh,c1_c2_fresh,c1_c3_fresh,c1_c4_fresh,c2_c2_fresh,c2_c3_fresh,c2_c4_fresh,c3_c3_fresh,c3_c4_fresh,c4_c4_fresh,"+
                            "c1_c1_repeat,c1_c2_repeat,c1_c3_repeat,c1_c4_repeat,c2_c2_repeat,c2_c3_repeat,"+
                            "c2_c4_repeat,c3_c3_repeat,c3_c4_repeat,c4_c4_repeat) VALUES ('" + issue_id + "',0,0,0,0,0,0,0,0,1,0,0,0,0,0,0,0,0,0,0,0);";
                    st2.executeUpdate(q2);
                } else if (interaction_type.equals("44")) {
                    String q2 = "INSERT INTO pqnBuilder(issue_id,c1_c1_fresh,c1_c2_fresh,c1_c3_fresh,c1_c4_fresh,c2_c2_fresh,c2_c3_fresh,c2_c4_fresh,c3_c3_fresh,c3_c4_fresh,c4_c4_fresh,"+
                            "c1_c1_repeat,c1_c2_repeat,c1_c3_repeat,c1_c4_repeat,c2_c2_repeat,c2_c3_repeat,"+
                            "c2_c4_repeat,c3_c3_repeat,c3_c4_repeat,c4_c4_repeat) VALUES ('" + issue_id + "',0,0,0,0,0,0,0,0,0,1,0,0,0,0,0,0,0,0,0,0);";
                    st2.executeUpdate(q2);
                }
            }
            else {
                if (interaction_type.equals("11")) {
                    int count = rs1.getInt("c1_c1_fresh");
                    count++;
                    String q2 = "update pqnBuilder set c1_c1_fresh =" + count + " where issue_id ='" + issue_id + "';";
                    st2.executeUpdate(q2);
                } else if (interaction_type.equals("12") || interaction_type.equals("21")) {
                    int count = rs1.getInt("c1_c2_fresh");
                    count++;
                    String q2 = "update pqnBuilder set c1_c2_fresh =" + count + " where issue_id ='" + issue_id + "';";
                    st2.executeUpdate(q2);
                } else if (interaction_type.equals("13") || interaction_type.equals("31")) {
                    int count = rs1.getInt("c1_c3_fresh");
                    count++;
                    String q2 = "update pqnBuilder set c1_c3_fresh =" + count + " where issue_id ='" + issue_id + "';";
                    st2.executeUpdate(q2);
                } else if (interaction_type.equals("14") || interaction_type.equals("41")) {
                    int count = rs1.getInt("c1_c4_fresh");
                    count++;
                    String q2 = "update pqnBuilder set c1_c4_fresh =" + count + " where issue_id ='" + issue_id + "';";
                    st2.executeUpdate(q2);
                } else if (interaction_type.equals("22")) {
                    int count = rs1.getInt("c2_c2_fresh");
                    count++;
                    String q2 = "update pqnBuilder set c2_c2_fresh =" + count + " where issue_id ='" + issue_id + "';";
                    st2.executeUpdate(q2);
                } else if (interaction_type.equals("23") || interaction_type.equals("32")) {
                    int count = rs1.getInt("c2_c3_fresh");
                    count++;
                    String q2 = "update pqnBuilder set c2_c3_fresh =" + count + " where issue_id ='" + issue_id + "';";
                    st2.executeUpdate(q2);
                } else if (interaction_type.equals("24") || interaction_type.equals("42")) {
                    int count = rs1.getInt("c2_c4_fresh");
                    count++;
                    String q2 = "update pqnBuilder set c2_c4_fresh =" + count + " where issue_id ='" + issue_id + "';";
                    st2.executeUpdate(q2);
                } else if (interaction_type.equals("33")) {
                    int count = rs1.getInt("c3_c3_fresh");
                    count++;
                    String q2 = "update pqnBuilder set c3_c3_fresh =" + count + " where issue_id ='" + issue_id + "';";
                    st2.executeUpdate(q2);
                } else if (interaction_type.equals("34") || interaction_type.equals("43")) {
                    int count = rs1.getInt("c3_c4_fresh");
                    count++;
                    String q2 = "update pqnBuilder set c3_c4_fresh =" + count + " where issue_id ='" + issue_id + "';";
                    st2.executeUpdate(q2);
                } else if (interaction_type.equals("44")) {
                    int count = rs1.getInt("c4_c4_fresh");
                    count++;
                    String q2 = "update pqnBuilder set c4_c4_fresh =" + count + " where issue_id ='" + issue_id + "';";
                    st2.executeUpdate(q2);
                }
            }
            rs1.close();
        }
        else if(condition.equals("repeat")){
            String q1 = "Select * from pqnBuilder where issue_id ='" + issue_id + "';";
            ResultSet rs1 = st1.executeQuery(q1);

            if (!rs1.next()) {
                if (interaction_type.equals("11")) {
                    String q2 = "INSERT INTO pqnBuilder(issue_id,c1_c1_fresh,c1_c2_fresh,c1_c3_fresh,c1_c4_fresh,c2_c2_fresh,c2_c3_fresh,c2_c4_fresh,c3_c3_fresh,c3_c4_fresh,c4_c4_fresh,"+
                            "c1_c1_repeat,c1_c2_repeat,c1_c3_repeat,c1_c4_repeat,c2_c2_repeat,c2_c3_repeat,"+
                            "c2_c4_repeat,c3_c3_repeat,c3_c4_repeat,c4_c4_repeat) VALUES ('" + issue_id + "',0,0,0,0,0,0,0,0,0,0,1,0,0,0,0,0,0,0,0,0);";
                    st2.executeUpdate(q2);
                } else if (interaction_type.equals("12") || interaction_type.equals("21")) {
                    String q2 = "INSERT INTO pqnBuilder(issue_id,c1_c1_fresh,c1_c2_fresh,c1_c3_fresh,c1_c4_fresh,c2_c2_fresh,c2_c3_fresh,c2_c4_fresh,c3_c3_fresh,c3_c4_fresh,c4_c4_fresh,"+
                            "c1_c1_repeat,c1_c2_repeat,c1_c3_repeat,c1_c4_repeat,c2_c2_repeat,c2_c3_repeat,"+
                            "c2_c4_repeat,c3_c3_repeat,c3_c4_repeat,c4_c4_repeat) VALUES ('" + issue_id + "',0,0,0,0,0,0,0,0,0,0,0,1,0,0,0,0,0,0,0,0);";
                    st2.executeUpdate(q2);
                } else if (interaction_type.equals("13") || interaction_type.equals("31")) {
                    String q2 = "INSERT INTO pqnBuilder(issue_id,c1_c1_fresh,c1_c2_fresh,c1_c3_fresh,c1_c4_fresh,c2_c2_fresh,c2_c3_fresh,c2_c4_fresh,c3_c3_fresh,c3_c4_fresh,c4_c4_fresh,"+
                            "c1_c1_repeat,c1_c2_repeat,c1_c3_repeat,c1_c4_repeat,c2_c2_repeat,c2_c3_repeat,"+
                            "c2_c4_repeat,c3_c3_repeat,c3_c4_repeat,c4_c4_repeat) VALUES ('" + issue_id + "',0,0,0,0,0,0,0,0,0,0,0,0,1,0,0,0,0,0,0,0);";
                    st2.executeUpdate(q2);
                } else if (interaction_type.equals("14") || interaction_type.equals("41")) {
                    String q2 = "INSERT INTO pqnBuilder(issue_id,c1_c1_fresh,c1_c2_fresh,c1_c3_fresh,c1_c4_fresh,c2_c2_fresh,c2_c3_fresh,c2_c4_fresh,c3_c3_fresh,c3_c4_fresh,c4_c4_fresh,"+
                            "c1_c1_repeat,c1_c2_repeat,c1_c3_repeat,c1_c4_repeat,c2_c2_repeat,c2_c3_repeat,"+
                            "c2_c4_repeat,c3_c3_repeat,c3_c4_repeat,c4_c4_repeat) VALUES ('" + issue_id + "',0,0,0,0,0,0,0,0,0,0,0,0,0,1,0,0,0,0,0,0);";
                    st2.executeUpdate(q2);
                } else if (interaction_type.equals("22")) {
                    String q2 = "INSERT INTO pqnBuilder(issue_id,c1_c1_fresh,c1_c2_fresh,c1_c3_fresh,c1_c4_fresh,c2_c2_fresh,c2_c3_fresh,c2_c4_fresh,c3_c3_fresh,c3_c4_fresh,c4_c4_fresh,"+
                            "c1_c1_repeat,c1_c2_repeat,c1_c3_repeat,c1_c4_repeat,c2_c2_repeat,c2_c3_repeat,"+
                            "c2_c4_repeat,c3_c3_repeat,c3_c4_repeat,c4_c4_repeat) VALUES ('" + issue_id + "',0,0,0,0,0,0,0,0,0,0,0,0,0,0,1,0,0,0,0,0);";
                    st2.executeUpdate(q2);
                } else if (interaction_type.equals("23") || interaction_type.equals("32")) {
                    String q2 = "INSERT INTO pqnBuilder(issue_id,c1_c1_fresh,c1_c2_fresh,c1_c3_fresh,c1_c4_fresh,c2_c2_fresh,c2_c3_fresh,c2_c4_fresh,c3_c3_fresh,c3_c4_fresh,c4_c4_fresh,"+
                            "c1_c1_repeat,c1_c2_repeat,c1_c3_repeat,c1_c4_repeat,c2_c2_repeat,c2_c3_repeat,"+
                            "c2_c4_repeat,c3_c3_repeat,c3_c4_repeat,c4_c4_repeat) VALUES ('" + issue_id + "',0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1,0,0,0,0);";
                    st2.executeUpdate(q2);
                } else if (interaction_type.equals("24") || interaction_type.equals("42")) {
                    String q2 = "INSERT INTO pqnBuilder(issue_id,c1_c1_fresh,c1_c2_fresh,c1_c3_fresh,c1_c4_fresh,c2_c2_fresh,c2_c3_fresh,c2_c4_fresh,c3_c3_fresh,c3_c4_fresh,c4_c4_fresh,"+
                            "c1_c1_repeat,c1_c2_repeat,c1_c3_repeat,c1_c4_repeat,c2_c2_repeat,c2_c3_repeat,"+
                            "c2_c4_repeat,c3_c3_repeat,c3_c4_repeat,c4_c4_repeat) VALUES ('" + issue_id + "',0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1,0,0,0);";
                    st2.executeUpdate(q2);
                } else if (interaction_type.equals("33")) {
                    String q2 = "INSERT INTO pqnBuilder(issue_id,c1_c1_fresh,c1_c2_fresh,c1_c3_fresh,c1_c4_fresh,c2_c2_fresh,c2_c3_fresh,c2_c4_fresh,c3_c3_fresh,c3_c4_fresh,c4_c4_fresh,"+
                            "c1_c1_repeat,c1_c2_repeat,c1_c3_repeat,c1_c4_repeat,c2_c2_repeat,c2_c3_repeat,"+
                            "c2_c4_repeat,c3_c3_repeat,c3_c4_repeat,c4_c4_repeat) VALUES ('" + issue_id + "',0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1,0,0);";
                    st2.executeUpdate(q2);
                } else if (interaction_type.equals("34") || interaction_type.equals("43")) {
                    String q2 = "INSERT INTO pqnBuilder(issue_id,c1_c1_fresh,c1_c2_fresh,c1_c3_fresh,c1_c4_fresh,c2_c2_fresh,c2_c3_fresh,c2_c4_fresh,c3_c3_fresh,c3_c4_fresh,c4_c4_fresh,"+
                            "c1_c1_repeat,c1_c2_repeat,c1_c3_repeat,c1_c4_repeat,c2_c2_repeat,c2_c3_repeat,"+
                            "c2_c4_repeat,c3_c3_repeat,c3_c4_repeat,c4_c4_repeat) VALUES ('" + issue_id + "',0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1,0);";
                    st2.executeUpdate(q2);
                } else if (interaction_type.equals("44")) {
                    String q2 = "INSERT INTO pqnBuilder(issue_id,c1_c1_fresh,c1_c2_fresh,c1_c3_fresh,c1_c4_fresh,c2_c2_fresh,c2_c3_fresh,c2_c4_fresh,c3_c3_fresh,c3_c4_fresh,c4_c4_fresh,"+
                            "c1_c1_repeat,c1_c2_repeat,c1_c3_repeat,c1_c4_repeat,c2_c2_repeat,c2_c3_repeat,"+
                            "c2_c4_repeat,c3_c3_repeat,c3_c4_repeat,c4_c4_repeat) VALUES ('" + issue_id + "',0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1);";
                    st2.executeUpdate(q2);
                }
            }
            else {
                if (interaction_type.equals("11")) {
                    int count = rs1.getInt("c1_c1_repeat");
                    count++;
                    String q2 = "update pqnBuilder set c1_c1_repeat =" + count + " where issue_id ='" + issue_id + "';";
                    st2.executeUpdate(q2);
                } else if (interaction_type.equals("12") || interaction_type.equals("21")) {
                    int count = rs1.getInt("c1_c2_repeat");
                    count++;
                    String q2 = "update pqnBuilder set c1_c2_repeat =" + count + " where issue_id ='" + issue_id + "';";
                    st2.executeUpdate(q2);
                } else if (interaction_type.equals("13") || interaction_type.equals("31")) {
                    int count = rs1.getInt("c1_c3_repeat");
                    count++;
                    String q2 = "update pqnBuilder set c1_c3_repeat =" + count + " where issue_id ='" + issue_id + "';";
                    st2.executeUpdate(q2);
                } else if (interaction_type.equals("14") || interaction_type.equals("41")) {
                    int count = rs1.getInt("c1_c4_repeat");
                    count++;
                    String q2 = "update pqnBuilder set c1_c4_repeat =" + count + " where issue_id ='" + issue_id + "';";
                    st2.executeUpdate(q2);
                } else if (interaction_type.equals("22")) {
                    int count = rs1.getInt("c2_c2_repeat");
                    count++;
                    String q2 = "update pqnBuilder set c2_c2_repeat =" + count + " where issue_id ='" + issue_id + "';";
                    st2.executeUpdate(q2);
                } else if (interaction_type.equals("23") || interaction_type.equals("32")) {
                    int count = rs1.getInt("c2_c3_repeat");
                    count++;
                    String q2 = "update pqnBuilder set c2_c3_repeat =" + count + " where issue_id ='" + issue_id + "';";
                    st2.executeUpdate(q2);
                } else if (interaction_type.equals("24") || interaction_type.equals("42")) {
                    int count = rs1.getInt("c2_c4_repeat");
                    count++;
                    String q2 = "update pqnBuilder set c2_c4_repeat =" + count + " where issue_id ='" + issue_id + "';";
                    st2.executeUpdate(q2);
                } else if (interaction_type.equals("33")) {
                    int count = rs1.getInt("c3_c3_repeat");
                    count++;
                    String q2 = "update pqnBuilder set c3_c3_repeat =" + count + " where issue_id ='" + issue_id + "';";
                    st2.executeUpdate(q2);
                } else if (interaction_type.equals("34") || interaction_type.equals("43")) {
                    int count = rs1.getInt("c3_c4_repeat");
                    count++;
                    String q2 = "update pqnBuilder set c3_c4_repeat =" + count + " where issue_id ='" + issue_id + "';";
                    st2.executeUpdate(q2);
                } else if (interaction_type.equals("44")) {
                    int count = rs1.getInt("c4_c4_repeat");
                    count++;
                    String q2 = "update pqnBuilder set c4_c4_repeat =" + count + " where issue_id ='" + issue_id + "';";
                    st2.executeUpdate(q2);
                }
            }
            rs1.close();
        }

        connection.close();
        st1.close();
        st2.close();
    }

    /*------------------------------------------------------------------------------------------------------------------------------------------------------------*/

    //Finally, from the Edges_OS and categoryTracker table the pajek file is generated.
    public void pajekFileGenerator() throws ClassNotFoundException, SQLException, IOException    {
        int pcount=1,edgeTotal=0,verTotal=0,slno=0,eweight=0; String dev1="",dev2="",dev1_slNo="",dev2_slNo="",dev="",edev1="",edev2="";
        Class.forName("com.mysql.cj.jdbc.Driver");
        Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/eclipse", "root", "Sayantak1");

        System.out.println("In pajek File generator");
        //Prepare table for pajek file formation

        Statement crtTab=conn.createStatement();
        String queryCrt="create table edges_pajek as select * from Edges_OS;";
        crtTab.executeUpdate(queryCrt);
        Statement stmt4=conn.createStatement();
        String query4="SELECT COUNT(*) as total_edges FROM edges_pajek;";
        ResultSet rs4=stmt4.executeQuery(query4);
        while(rs4.next()) {edgeTotal=Integer.parseInt(rs4.getString("total_edges")); }
        // System.out.println("Total is ++++++++++++++++++++++++ "+edgeTotal);

        Statement stmt5=conn.createStatement();
        String query5="SELECT COUNT(*) as total_vertices FROM categoryTracker;";
        ResultSet rs5=stmt5.executeQuery(query5);
        while(rs5.next()) {verTotal=Integer.parseInt(rs5.getString("total_vertices")); }
        //  System.out.println("Total is ++++++++++++++++++++++++ "+verTotal);


        // File Writing starts
        System.out.println("File Writing starts");
        Statement stmt6=conn.createStatement();
        FileWriter writer = new FileWriter("PajekFileEclipse.net", false);
        String lineFirst="*Vertices"+" "+verTotal;
        writer.write(lineFirst);
        writer.write("\r\n");
        System.out.println(" Writing Vertices");
        for(int i=1;i<=verTotal;i++)
        {

            String query6="SELECT slno,dev_id FROM categoryTracker WHERE SLNO="+i+";";
            ResultSet rs6=stmt6.executeQuery(query6);
            while(rs6.next()) {slno=Integer.parseInt(rs6.getString("slno")); dev=rs6.getString("dev_id");}
            String lineVertices=slno+" "+"\""+dev+"\"";
            writer.write(lineVertices);
            writer.write("\r\n");
            dev="";
            rs6.close();
        }
        String line2="*Edges";
        writer.write(line2);
        writer.write("\r\n");
        System.out.println(" Edges");

        for(int j=1;j<=edgeTotal;j++) {
            Statement stmt7=conn.createStatement();
            String query7="SELECT dev1,dev2,weight FROM edges_pajek WHERE slno="+j+";";
            ResultSet rs7=stmt7.executeQuery(query7);
            while(rs7.next()) {edev1=rs7.getString("dev1"); edev2=rs7.getString("dev2"); eweight=rs7.getInt("weight");}
            String lineEdges=edev1+" "+edev2+" "+eweight;
            writer.write(lineEdges);
            writer.write("\r\n");
            edev1="";edev2="";
            rs7.close();
            stmt7.close();
        }
        conn.close();
        writer.close();
        rs4.close();
        rs5.close();
        stmt4.close();
        stmt5.close();
        stmt6.close();
    }


    /**------------------------------------------------------------------------------------------------------------------------------------------------------------------**/

    public static void main (String[] args) throws ClassNotFoundException, SQLException, IOException {

        Scanner sc=new Scanner (System.in);
        System.out.print("Enter the number of Categories you want : ");
        n=sc.nextInt();

        Class.forName("com.mysql.cj.jdbc.Driver");
        Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/eclipse", "root", "Sayantak1");

        String query1="drop table if exists categoryTracker;";
        String query2="drop table if exists vertices;";
        String query3="drop table if exists Edges_OS;";
        String query4="drop table if exists edges_pajek;";
        String query5="drop table if exists pqnBuilder;";
        String query6="drop table if exists repeat_edges;";

        String q1="Create table categoryTracker(" +
                "slno INT ," +
                "dev_id varchar(20)," +
                "comment_count INT, " +
                "category VARCHAR(4))";

        String q2="CREATE TABLE vertices (" +
                "dev_id varchar(20)," +
                "issue_id VARCHAR(20),"+
                "no_of_comments INT)";

        String q3="CREATE TABLE Edges_OS (" +
                "slno INT ,"+
                "dev1 varchar(20) ," +
                "dev2 varchar(20)," +
                "interaction_type varchar(4)," +
                "weight INT)";

        String q4="create table pqnBuilder(" +
                "issue_id varchar(20)," +
                "c1_c1_fresh INT, c1_c2_fresh INT ,c1_c3_fresh INT ,c1_c4_fresh INT,c2_c2_fresh INT, c2_c3_fresh INT, c2_c4_fresh INT, c3_c3_fresh INT, c3_c4_fresh INT, c4_c4_fresh INT," +
                "c1_c1_repeat INT, c1_c2_repeat INT ,c1_c3_repeat INT ,c1_c4_repeat INT,c2_c2_repeat INT, c2_c3_repeat INT, c2_c4_repeat INT, c3_c3_repeat INT, c3_c4_repeat INT, c4_c4_repeat INT" +
                ")";

        String q5="create table repeat_edges(" +
                "issue_id varchar(20)," +
                "dev1 varchar(20) ," +
                "dev2 varchar(20)," +
                "repeat_edges INT" +
                ")";



        Statement st1=conn.createStatement();

        st1.executeUpdate(query1);
        st1.executeUpdate(query2);
        st1.executeUpdate(query3);
        st1.executeUpdate(query4);
        st1.executeUpdate(query5);
        st1.executeUpdate(query6);

        System.out.println(" Creating Table categoryTracker :\n");
        st1.executeUpdate(q1);
        System.out.println(" Creating Table vertices :\n");
        st1.executeUpdate(q2);
        System.out.println(" Creating Table Edges_OS :\n");
        st1.executeUpdate(q3);
        System.out.println(" Creating Table pqnBuilder :\n");
        st1.executeUpdate(q4);
        System.out.println(" Creating Table repeat_edges :\n");
        st1.executeUpdate(q5);

        conn.close();
        st1.close();

        Interaction_Network_Eclipse obj =new Interaction_Network_Eclipse();

        obj.categoryDecider();
        obj.read_CSVFile();
        obj.pajekFileGenerator();
    }
}
