public class FlightBookingFacade {
    private BaggagePricingEngine baggagePricingEngine;
    private CurrencyConverter currencyConverter;
    private PaymentProcessor paymentProcessor;
    private SeatInventorySystem seatInventorySystem;
    private TicketIssuer ticketIssuer;

    public FlightBookingFacade(){
        this.baggagePricingEngine = new BaggagePricingEngine();
        this.currencyConverter = new CurrencyConverter();
        this.paymentProcessor = new PaymentProcessor();
        this.ticketIssuer = new TicketIssuer();
        this.seatInventorySystem = new SeatInventorySystem();
    }


    public String bookFlight(String passengerName,
                             String flightCode,
                             String seatNumber,
                             double baseFareUSD,
                             int bagCount,
                             double bagWeightKg,
                             String currency,
                             String cardNumber,
                             String loyaltyTier) {
                                String lockToken = null;

                                try{
                                    seatInventorySystem.connectToReservationServer();
                                    lockToken = seatInventorySystem.lockSeat(flightCode, seatNumber, passengerName);
                                    double baggageFee = 0.0;
                                    if (!"SILVER".equalsIgnoreCase(loyaltyTier) && !"GOLD".equalsIgnoreCase(loyaltyTier)) {
                                        baggageFee = baggagePricingEngine.calculateBaggageFeeUSD(bagCount, bagWeightKg);
                                    }
                                    double totalUSD = baseFareUSD + baggageFee;
                                    double convertedTotal = currencyConverter.convertFromUSD(totalUSD, currency);
                                    paymentProcessor.initializeGateway();
                                    if ("SILVER".equalsIgnoreCase(loyaltyTier)) {
                                        paymentProcessor.applyPromoCode("SILVER-FREE-BAG");
                                    }
                                    String receiptId = paymentProcessor.chargeCard(cardNumber, convertedTotal, currency);
                                    return ticketIssuer.generateTicket(passengerName, flightCode, lockToken, receiptId);
                                }
                                catch(RuntimeException ex){
                                    if (lockToken != null) {
                                        seatInventorySystem.releaseSeatLock(lockToken);
                                    }
                                    System.out.println("[Facade] Booking failed for " + passengerName + ": " + ex.getMessage());
                                    return "BOOKING_FAILED: " + ex.getMessage();
                                }
                                finally {
                                    seatInventorySystem.disconnect();
                                }
    }


    public String bookFlight(String passengerName, String flightCode, String seatNumber,
                             double baseFareUSD, int bagCount, double bagWeightKg,
                             String currency, String cardNumber) {
        return bookFlight(passengerName, flightCode, seatNumber, baseFareUSD,
                bagCount, bagWeightKg, currency, cardNumber, "STANDARD");
    }

}
