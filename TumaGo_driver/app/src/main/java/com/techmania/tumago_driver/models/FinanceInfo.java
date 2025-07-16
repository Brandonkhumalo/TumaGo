package com.techmania.tumago_driver.models;

public class FinanceInfo {

    public FinancePeriod today;
    public FinancePeriod week;
    public FinancePeriod month;
    public FinancePeriod all_time;

    public static class FinancePeriod {
        private String earnings;
        private String charges;
        private String profit;
        private int total_trips;

        public String getEarnings() { return earnings; }
        public String getCharges() { return charges; }
        public String getProfit() { return profit; }
        public int getTotalTrips() { return total_trips; }
    }

    public FinancePeriod getToday() { return today; }
    public FinancePeriod getWeek() { return week; }
    public FinancePeriod getMonth() { return month; }
    public FinancePeriod getAllTime() { return all_time; }
}
