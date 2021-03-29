package uk.tw.energy.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.tw.energy.domain.PricePlan;

import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;

class PricePlanServiceTest {

    private  PricePlanService pricePlanService;
    private MeterReadingService meterReadingService;
    private PricePlan pricePlan;

    @BeforeEach
    public void setUp() {
        meterReadingService = new MeterReadingService(new HashMap<>());

    }


}