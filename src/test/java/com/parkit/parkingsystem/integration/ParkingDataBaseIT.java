package com.parkit.parkingsystem.integration;

import com.parkit.parkingsystem.constants.Fare;
import com.parkit.parkingsystem.constants.ParkingType;
import com.parkit.parkingsystem.dao.ParkingSpotDAO;
import com.parkit.parkingsystem.dao.TicketDAO;
import com.parkit.parkingsystem.integration.config.DataBaseTestConfig;
import com.parkit.parkingsystem.integration.service.DataBasePrepareService;
import com.parkit.parkingsystem.model.ParkingSpot;
import com.parkit.parkingsystem.model.Ticket;
import com.parkit.parkingsystem.service.ParkingService;
import com.parkit.parkingsystem.util.InputReaderUtil;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ParkingDataBaseIT {

    private static DataBaseTestConfig dataBaseTestConfig = new DataBaseTestConfig();
    private static ParkingSpotDAO parkingSpotDAO;
    private static TicketDAO ticketDAO;
    private static DataBasePrepareService dataBasePrepareService;

    private long IN_TIME_SYSTEM;
    private long NEW_OUT_TIME;

    @Mock
    private static InputReaderUtil inputReaderUtil;

    @BeforeAll
    private static void setUp() throws Exception {
        parkingSpotDAO = new ParkingSpotDAO();
        parkingSpotDAO.dataBaseConfig = dataBaseTestConfig;
        ticketDAO = new TicketDAO();
        ticketDAO.dataBaseConfig = dataBaseTestConfig;
        dataBasePrepareService = new DataBasePrepareService();
    }

    @BeforeEach
    private void setUpPerTest() throws Exception {
        when(inputReaderUtil.readSelection()).thenReturn(1);
        when(inputReaderUtil.readVehicleRegistrationNumber()).thenReturn("ABCDEF");
        dataBasePrepareService.clearDataBaseEntries();

        IN_TIME_SYSTEM = System.currentTimeMillis();
        NEW_OUT_TIME = IN_TIME_SYSTEM + (3600 * 1000);

    }

    @AfterAll
    private static void tearDown(){

    }

    @Test
    public void testParkingACar() throws Exception {

        // GIVEN
        ParkingService parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);

        // WHEN
        parkingService.processIncomingVehicle();

        // THEN
        Ticket ticket = ticketDAO.getTicket(inputReaderUtil.readVehicleRegistrationNumber());
        boolean isParkingAvailable = ticket.getParkingSpot().isAvailable();
        int slotNumber = parkingSpotDAO.getNextAvailableSlot(ParkingType.CAR);

        assertAll("Error during parking car entry test",
                () -> Assertions.assertNull((ticket.getOutTime()), "Error with ticket out time. Can't be null."),
                () -> Assertions.assertEquals(false, isParkingAvailable, "Error with slot available, not true."),
                () -> Assertions.assertEquals(2, slotNumber, "Error with parking table not update in DB."),
                () -> Assertions.assertNotNull(ticket, "Error, ticket table not update in DB."));
    }

    @Test
    public void testParkingLotExit() throws Exception {
        testParkingACar();

        // GIVEN
        ParkingService parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);

        // WHEN
        parkingService.processExitingVehicle();

        Ticket ticket = ticketDAO.getTicket(inputReaderUtil.readVehicleRegistrationNumber());
        int parkingSpot = parkingSpotDAO.getNextAvailableSlot(ParkingType.CAR);

        // THEN
        assertAll("Error during parking car exit test",
                () -> Assertions.assertEquals(1, parkingSpot, "Error with parking table not update in DB."),
                () -> Assertions.assertNotNull((ticket.getOutTime()), "Error with the ticket out time which is null."));
    }

    @Test
    public void testParkingLotExitRecurringUser() throws Exception {

        // GIVEN
        ParkingService parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);

        String vehicleRegNumber = inputReaderUtil.readVehicleRegistrationNumber();

        // WHEN
        parkingService.processIncomingVehicle();

        Ticket ticket = ticketDAO.getTicket(vehicleRegNumber);
        ticket.setInTime(new Date(System.currentTimeMillis()));
        ticket.setOutTime(new Date(System.currentTimeMillis() + 60 * 60 * 1000));
        ticketDAO.updateTicket(ticket);

        parkingService.processExitingVehicle();
        parkingService.processIncomingVehicle();

        Ticket secondTicket = ticketDAO.getTicket(vehicleRegNumber);
        secondTicket.setOutTime(new Date(System.currentTimeMillis() + 60 * 60 * 1000));
        ticketDAO.updateTicket(secondTicket);

        parkingService.processExitingVehicle();

        // THEN
        ticket = ticketDAO.getTicket(vehicleRegNumber);

        double expectedFare =
                Math.round(Fare.CAR_RATE_PER_HOUR * 0.95 * 100.0) / 100.0;

        assertNotNull(ticket.getOutTime());
        assertEquals(expectedFare, ticket.getPrice(), 0.01, "5% discount for a recurring user.");

        parkingService.processExitingVehicle();
    }
}
