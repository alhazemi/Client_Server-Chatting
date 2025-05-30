USE [Bankdb]
GO

/****** Object:  StoredProcedure [dbo].[TransferFund]    Script Date: 5/25/2025 09:58:44 م ******/
SET ANSI_NULLS ON
GO

SET QUOTED_IDENTIFIER ON
GO



CREATE PROCEDURE [dbo].[TransferFund]
    @SenderAccountNumber   varchar(20),
    @ReceiverAccountNumber varchar(20),
    @Amount            DECIMAL(10,2),
	@Description varchar(100),
	@AccountID int,
	@AccountIDRes int
AS
BEGIN
		IF NOT EXISTS (
            SELECT 1 
            FROM Accounts 
            WHERE AccountNumber = @SenderAccountNumber
              
			  
        )
		begin
		return 2;
		end

		IF NOT EXISTS (
            SELECT 1 
            FROM Accounts 
            WHERE AccountNumber = @ReceiverAccountNumber
              
			  
        )
		begin
		return 3;
		end

        IF NOT EXISTS (
            SELECT 1 
            FROM Accounts 
            WHERE AccountNumber = @SenderAccountNumber
              AND Balance >= @Amount
			  
        )
		begin
		return 1;
		end
        UPDATE Accounts
        SET Balance = Balance - @Amount
        WHERE AccountNumber = @SenderAccountNumber;


        UPDATE Accounts
        SET Balance = Balance + @Amount
        WHERE AccountNumber = @ReceiverAccountNumber;

        INSERT INTO Transactions
            (AccountID, EmployeeID, TransactionType, Amount, Description)
        VALUES
            (@AccountID,NULL, N'تحويل - خصم', @Amount,@Description+ CAST(@ReceiverAccountNumber AS NVARCHAR(20))),

            (@AccountIDRes,NULL, N'تحويل - إضافة', @Amount,@Description + CAST(@SenderAccountNumber AS NVARCHAR(20)));

	return 0;
   
END;
GO

