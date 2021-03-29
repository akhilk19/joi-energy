package uk.tw.energy.service;

import org.springframework.stereotype.Service;
import uk.tw.energy.domain.ElectricityReading;
import uk.tw.energy.domain.PricePlan;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class PricePlanService {

    private final List<PricePlan> pricePlans;
    private  Map<String, String> smartMeterToPricePlanAccounts = null;
    private final MeterReadingService meterReadingService;

    public PricePlanService(List<PricePlan> pricePlans, MeterReadingService meterReadingService) {
        this.pricePlans = pricePlans;
        this.meterReadingService = meterReadingService;
    }
    public PricePlanService(List<PricePlan> pricePlans, MeterReadingService meterReadingService, Map<String,String> smartMeterToPricePlanAccounts) {
        this.pricePlans = pricePlans;
        this.meterReadingService = meterReadingService;
        this.smartMeterToPricePlanAccounts = smartMeterToPricePlanAccounts;
    }

    public Optional<Map<String, BigDecimal>> getConsumptionCostOfElectricityReadingsForEachPricePlan(String smartMeterId) {
        Optional<List<ElectricityReading>> electricityReadings = meterReadingService.getReadings(smartMeterId);

        if (!electricityReadings.isPresent()) {
            return Optional.empty();
        }

        return Optional.of(pricePlans.stream().collect(
                Collectors.toMap(PricePlan::getPlanName, t -> calculateCost(electricityReadings.get(), t))));
    }

    private BigDecimal calculateCost(List<ElectricityReading> electricityReadings, PricePlan pricePlan) {
        BigDecimal average = calculateAverageReading(electricityReadings);
        BigDecimal timeElapsed = calculateTimeElapsed(electricityReadings);

        BigDecimal averagedCost = average.divide(timeElapsed, RoundingMode.HALF_UP);
        return averagedCost.multiply(pricePlan.getUnitRate());
    }

    private BigDecimal calculateAverageReading(List<ElectricityReading> electricityReadings) {
        BigDecimal summedReadings = electricityReadings.stream()
                .map(ElectricityReading::getReading)
                .reduce(BigDecimal.ZERO, (reading, accumulator) -> reading.add(accumulator));

        return summedReadings.divide(BigDecimal.valueOf(electricityReadings.size()), RoundingMode.HALF_UP);
    }

    private BigDecimal calculateTimeElapsed(List<ElectricityReading> electricityReadings) {
        ElectricityReading first = electricityReadings.stream()
                .min(Comparator.comparing(ElectricityReading::getTime))
                .get();
        ElectricityReading last = electricityReadings.stream()
                .max(Comparator.comparing(ElectricityReading::getTime))
                .get();

        return BigDecimal.valueOf(Duration.between(first.getTime(), last.getTime()).getSeconds() / 3600.0);
    }
//  Newly added methods
    public Map<String, Object> getUsageCostsForMeterId(String smartMeterId) {
        Map<String, Object> responseBean = new HashMap<>();
        boolean isPricePlanAvailable = smartMeterToPricePlanAccounts.containsKey(smartMeterId);
        if(isPricePlanAvailable){
            Optional<List<ElectricityReading>> readingsForMeterId = meterReadingService.getReadings(smartMeterId);
            List<ElectricityReading> readingForLastWeek = getPastWeekElectricityReadings(readingsForMeterId);
            BigDecimal averageUsageReadings = calculateAverageReading(readingForLastWeek);
            BigDecimal timeElapsed = calculateTimeElapsed(readingForLastWeek);
            BigDecimal cost = averageUsageReadings.multiply(timeElapsed);
            responseBean.put("Cost",cost);
        } else {
            responseBean = null;
        }
        return responseBean;
    }
// Filters the electricity readings.
    private List<ElectricityReading> getPastWeekElectricityReadings(Optional<List<ElectricityReading>> readings) {
        List<ElectricityReading> filteredList = new ArrayList<>();
        if (readings.isPresent()) {
            List<ElectricityReading> readingsList = readings.get();

            Date currentDate = new Date();
            Date lastDate = new Date(Instant.now().minus(7, ChronoUnit.DAYS).toEpochMilli());
            //Rewritten and replaces for with streams as part of code pairing round
            filteredList = readingsList.stream().filter(item -> (item.getTime().isAfter(lastDate.toInstant()) && item.getTime().isBefore(currentDate.toInstant())))
                    .collect(Collectors.toList());
//            Commented as the above code finishes the requirement.
//            for (ElectricityReading item : readingsList) {
//                if (item.getTime().isAfter(lastDate.toInstant()) && item.getTime().isBefore(currentDate.toInstant())) {
//                    filteredList.add(item);
//                }
//            }
        }
        return filteredList;
    }
}
