package com.parkit.parkingsystem.integration.dao;

import com.parkit.parkingsystem.config.DataBaseConfig;
import com.parkit.parkingsystem.constants.DBConstants;
import com.parkit.parkingsystem.constants.ParkingType;
import com.parkit.parkingsystem.dao.TicketDAO;
import com.parkit.parkingsystem.model.ParkingSpot;
import com.parkit.parkingsystem.model.Ticket;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TicketDAOTest {

    @InjectMocks
    private TicketDAO ticketDAO;

    @Mock
    private DataBaseConfig dataBaseConfig;

    @Mock
    private Connection connection;

    @Mock
    private PreparedStatement preparedStatement;

    @Mock
    private ResultSet resultSet;

    @BeforeEach
    void setUp() throws Exception {
        when(dataBaseConfig.getConnection()).thenReturn(connection);
    }

    @Test
    void saveTicketShouldReturnFalseDueToFinallyBlock() throws Exception {

        // GIVEN
        Ticket ticket = new Ticket();
        ticket.setParkingSpot(new ParkingSpot(1, ParkingType.CAR, false));
        ticket.setVehicleRegNumber("AA-123-BB");
        ticket.setPrice(5.0);
        ticket.setInTime(new Date());

        when(connection.prepareStatement(DBConstants.SAVE_TICKET)).thenReturn(preparedStatement);
        when(preparedStatement.execute()).thenReturn(true);

        // WHEN
        boolean result = ticketDAO.saveTicket(ticket);

        // THEN
        assertFalse(result);

        verify(preparedStatement).setInt(1, 1);
        verify(preparedStatement).setString(2, "AA-123-BB");
        verify(preparedStatement).setDouble(3, 5.0);
        verify(preparedStatement).setTimestamp(eq(4), any(Timestamp.class));
        verify(preparedStatement).setTimestamp(eq(5), isNull());
        verify(dataBaseConfig).closeConnection(connection);
    }

    @Test
    void getTicketShouldReturnTicketWhenFound() throws Exception {

        // GIVEN
        when(connection.prepareStatement(DBConstants.GET_TICKET)).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);

        when(resultSet.getInt(1)).thenReturn(3);
        when(resultSet.getInt(2)).thenReturn(10);
        when(resultSet.getDouble(3)).thenReturn(20.0);
        when(resultSet.getTimestamp(4)).thenReturn(new Timestamp(System.currentTimeMillis()));
        when(resultSet.getTimestamp(5)).thenReturn(null);
        when(resultSet.getString(6)).thenReturn("CAR");

        // WHEN
        Ticket ticket = ticketDAO.getTicket("AA-123-BB");

        // THEN
        assertNotNull(ticket);
        assertEquals(10, ticket.getId());
        assertEquals("AA-123-BB", ticket.getVehicleRegNumber());
        assertEquals(20.0, ticket.getPrice());
        assertEquals(3, ticket.getParkingSpot().getId());
        assertEquals(ParkingType.CAR, ticket.getParkingSpot().getParkingType());

        verify(dataBaseConfig).closeResultSet(resultSet);
        verify(dataBaseConfig).closePreparedStatement(preparedStatement);
        verify(dataBaseConfig).closeConnection(connection);
    }

    @Test
    void getTicketShouldReturnNullWhenNotFound() throws Exception {

        // GIVEN
        when(connection.prepareStatement(DBConstants.GET_TICKET)).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(false);

        // WHEN
        Ticket ticket = ticketDAO.getTicket("UNKNOWN");

        // THEN
        assertNull(ticket);
        verify(dataBaseConfig).closeConnection(connection);
    }

    @Test
    void updateTicketShouldReturnTrueWhenUpdateSucceeds() throws Exception {

        // GIVEN
        Ticket ticket = new Ticket();
        ticket.setId(5);
        ticket.setPrice(30.0);
        ticket.setOutTime(new Date());

        when(connection.prepareStatement(DBConstants.UPDATE_TICKET)).thenReturn(preparedStatement);

        // WHEN
        boolean result = ticketDAO.updateTicket(ticket);

        // THEN
        assertTrue(result);
        verify(preparedStatement).setDouble(1, 30.0);
        verify(preparedStatement).setTimestamp(eq(2), any(Timestamp.class));
        verify(preparedStatement).setInt(3, 5);
        verify(dataBaseConfig).closeConnection(connection);
    }

    @Test
    void updateTicketShouldReturnFalseWhenExceptionOccurs() throws Exception {

        // GIVEN
        Ticket ticket = new Ticket();
        ticket.setId(1);
        ticket.setOutTime(new Date());

        when(dataBaseConfig.getConnection()).thenThrow(new RuntimeException("DB error"));

        // WHEN
        boolean result = ticketDAO.updateTicket(ticket);

        // THEN
        assertFalse(result);
    }

    @Test
    void getNbTicketShouldReturnCountWhenFound() throws Exception {

        // GIVEN
        when(connection.prepareStatement(DBConstants.GET_NB_TICKET)).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);
        when(resultSet.getInt(1)).thenReturn(4);

        // WHEN
        int count = ticketDAO.getNbTicket("AA-123-BB");

        // THEN
        assertEquals(4, count);
        verify(dataBaseConfig).closeResultSet(resultSet);
        verify(dataBaseConfig).closePreparedStatement(preparedStatement);
        verify(dataBaseConfig).closeConnection(connection);
    }

    @Test
    void getNbTicketShouldReturnZeroWhenExceptionOccurs() throws Exception {

        // GIVEN
        when(dataBaseConfig.getConnection()).thenThrow(new RuntimeException("DB error"));

        // WHEN
        int count = ticketDAO.getNbTicket("AA-123-BB");

        // THEN
        assertEquals(0, count);
    }
}
