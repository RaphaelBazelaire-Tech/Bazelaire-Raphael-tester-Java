package com.parkit.parkingsystem.integration.dao;

import com.parkit.parkingsystem.config.DataBaseConfig;
import com.parkit.parkingsystem.constants.DBConstants;
import com.parkit.parkingsystem.constants.ParkingType;
import com.parkit.parkingsystem.dao.ParkingSpotDAO;
import com.parkit.parkingsystem.model.ParkingSpot;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ParkingSpotDAOTest {

    @InjectMocks
    private ParkingSpotDAO parkingSpotDAO;

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
    void getNextAvailableSlotShouldReturnSlotIdWhenAvailable() throws Exception {

        // GIVEN
        when(connection.prepareStatement(DBConstants.GET_NEXT_PARKING_SPOT)).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);
        when(resultSet.getInt(1)).thenReturn(5);

        // WHEN
        int slot = parkingSpotDAO.getNextAvailableSlot(ParkingType.CAR);

        // THEN
        assertEquals(5, slot);
        verify(preparedStatement).setString(1, ParkingType.CAR.toString());
        verify(dataBaseConfig).closeResultSet(resultSet);
        verify(dataBaseConfig).closePreparedStatement(preparedStatement);
        verify(dataBaseConfig).closeConnection(connection);
    }

    @Test
    void getNextAvailableSlotShouldReturnMinusOneWhenNoResult() throws Exception {

        // GIVEN
        when(connection.prepareStatement(DBConstants.GET_NEXT_PARKING_SPOT)).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(false);

        // WHEN
        int slot = parkingSpotDAO.getNextAvailableSlot(ParkingType.BIKE);

        // THEN
        assertEquals(-1, slot);
        verify(dataBaseConfig).closeConnection(connection);
    }

    @Test
    void getNextAvailableSlotShouldReturnMinusOneWhenExceptionOccurs() throws Exception {

        // GIVEN
        when(dataBaseConfig.getConnection()).thenThrow(new RuntimeException("DB error"));

        // WHEN
        int slot = parkingSpotDAO.getNextAvailableSlot(ParkingType.CAR);

        // THEN
        assertEquals(-1, slot);
    }

    @Test
    void updateParkingShouldReturnTrueWhenUpdateIsSuccessful() throws Exception {

        // GIVEN
        ParkingSpot spot = new ParkingSpot(1, ParkingType.CAR, false);

        when(connection.prepareStatement(DBConstants.UPDATE_PARKING_SPOT)).thenReturn(preparedStatement);
        when(preparedStatement.executeUpdate()).thenReturn(1);

        // WHEN
        boolean result = parkingSpotDAO.updateParking(spot);

        // THEN
        assertTrue(result);
        verify(preparedStatement).setBoolean(1, false);
        verify(preparedStatement).setInt(2, 1);
        verify(dataBaseConfig).closePreparedStatement(preparedStatement);
        verify(dataBaseConfig).closeConnection(connection);
    }

    @Test
    void updateParkingShouldReturnFalseWhenNoRowUpdated() throws Exception {

        // GIVEN
        ParkingSpot spot = new ParkingSpot(1, ParkingType.CAR, true);

        when(connection.prepareStatement(DBConstants.UPDATE_PARKING_SPOT)).thenReturn(preparedStatement);
        when(preparedStatement.executeUpdate()).thenReturn(0);

        // WHEN
        boolean result = parkingSpotDAO.updateParking(spot);

        // THEN
        assertFalse(result);
    }

    @Test
    void updateParkingShouldReturnFalseWhenExceptionOccurs() throws Exception {

        // GIVEN
        ParkingSpot spot = new ParkingSpot(1, ParkingType.CAR, true);
        when(dataBaseConfig.getConnection()).thenThrow(new RuntimeException("DB error"));

        // WHEN
        boolean result = parkingSpotDAO.updateParking(spot);

        // THEN
        assertFalse(result);
    }
}
