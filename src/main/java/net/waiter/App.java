package net.waiter;

import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Jdbi;
import spark.ModelAndView;
import spark.template.handlebars.HandlebarsTemplateEngine;

import java.sql.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static spark.Spark.*;

public class App {

    static Map<String, Object> map = new HashMap<>();

    static int getHerokuAssignedPort() {
        ProcessBuilder processBuilder = new ProcessBuilder();
        if (processBuilder.environment().get("PORT") != null) {
            return Integer.parseInt(processBuilder.environment().get("PORT"));
        }
        return 8080; //return default port if heroku-port isn't set (i.e. on localhost)
    }

    public static void main(String[] args) throws SQLException {


        String dbDiskURL = "jdbc:sqlite:file:./Coffee_shop.db";
        Jdbi jdbi = Jdbi.create(dbDiskURL);

// get a handle to the database
        Handle handle = jdbi.open();

// create the table if needed
        handle.execute("create table if not exists waiters ( id INTEGER primary key AUTOINCREMENT, waiter varchar(15))");
        handle.execute("drop table if exists week_day");
//        handle.execute("drop table week_day");
        handle.execute("create table if not exists week_day ( id integer primary key AUTOINCREMENT,\n" +
                "weekday text\n" +
                "order_by integer)");


        handle.execute("INSERT INTO week_day(weekday) VALUES('Sunday')");
        handle.execute("INSERT INTO week_day(weekday) VALUES('Monday')");
        handle.execute("INSERT INTO week_day(weekday) VALUES('Tuesday')");
        handle.execute("INSERT INTO week_day(weekday) VALUES('Wednesday')");
        handle.execute("INSERT INTO week_day(weekday) VALUES('Thursday')");
        handle.execute("INSERT INTO week_day(weekday) VALUES('Friday')");
        handle.execute("INSERT INTO week_day(weekday) VALUES('Saturday')");


        handle.execute("create table if not exists shifts ( \n" +
                "\tid integer primary key AUTOINCREMENT,\n" +
                "\tweek_id integer,\n" +
                "\twaiter_id integer,\n" +
                "\tunique(week_id, waiter_id)\n" +
                ")");



        port(8080); // Spark will run on port 8080

        get("/waiter", (req, res) -> {
            Map<String, Object> map = new HashMap<>();

            return new ModelAndView(map, "login.handlebars");


        }, new HandlebarsTemplateEngine());


        get("/waiter/:username", (req, res) -> {
            Map<String, Object> map = new HashMap<>();

            String WaiterName = req.params(":username");


            List<Days> week = handle
                    .select("select weekday from week_day")
                    .mapToBean(Days.class)
                    .list();

            String waiterID = String.valueOf(handle.createQuery("select id from waiters where waiter= :waiter")
                    .bind("waiter", WaiterName)
                    .mapTo(String.class)
                    .list());


            map.put("WaiterName", WaiterName);
            map.put("waiterID", waiterID);
                map.put("week",week);
            System.out.println(waiterID);
            return new ModelAndView(map, "shift.handlebars");
        }, new HandlebarsTemplateEngine());


        post("/waiter/:username", (req, res) -> {
            Map<String, Object> map = new HashMap<>();
            String WaiterName = req.params(":username");
            String weekDays = req.queryParams("day");



            handle.execute("insert into shifts (waiter_id, week_id) values(?,?)", weekDays);
            map.put("weekDays", weekDays);
            return new ModelAndView(map, "shift.handlebars");
        }, new HandlebarsTemplateEngine());


        get("/manager", (req, res) -> {
            Map<String, Object> map = new HashMap<>();

             List<String> schedule = handle
                    .select("SELECT waiters.waiter, week_day.weekday from waiters INNER JOIN shifts on waiters.id = shifts.waiter_id INNER JOIN week_day on week_day.id = shifts.week_id;")
                    .mapTo(String.class)
                    .list();

            map.put("schedule", schedule);
            System.out.println(schedule);

            return new ModelAndView(map, "manager.handlebars");
        }, new HandlebarsTemplateEngine());


        post("/manager", (req, res) -> {

            Map<String, Object> map = new HashMap<>();

            String days = req.queryParams("manager");
            String WaiterName = req.queryParams("username");


            List<String> waiters = handle.select("select waiter from waiters where waiter = ?", WaiterName)
                    .mapTo(String.class)
                    .list();

            handle.execute("select weekday from week_day",days);

            map.put("manager", days);
            map.put("waiters", waiters);
            map.put("WaiterName", WaiterName);
            System.out.println(waiters);
            return new ModelAndView(map, "manager.handlebars");
        }, new HandlebarsTemplateEngine());
        
    }
}
