USE [Bankdb]
GO

/****** Object:  StoredProcedure [dbo].[WithdrawFromAccounts]    Script Date: 5/25/2025 09:59:21 م ******/
SET ANSI_NULLS ON
GO

SET QUOTED_IDENTIFIER ON
GO


CREATE PROCEDURE [dbo].[WithdrawFromAccounts]
    @AccountNumber varchar(20),
    @Amount    DECIMAL(10,2),
	@AccountID int
AS
BEGIN

		IF NOT EXISTS (
            SELECT 1 
            FROM Accounts 
            WHERE AccountNumber = @AccountNumber

        )
		begin
		return 0;
		end

        
        IF NOT EXISTS (
            SELECT 1 
            FROM dbo.Accounts 
            WHERE AccountNumber = @AccountNumber 
              AND Balance >= @Amount
        )
        BEGIN
            return 1;
        END
        UPDATE Accounts
        SET Balance = Balance - @Amount
        WHERE AccountNumber = @AccountNumber;

        INSERT INTO Transactions
            (AccountID, EmployeeID, TransactionType, Amount, Description)
        VALUES
            (@AccountID,NULL, N'سحب', @Amount, N'سحب نقدي');
		return 2;
        
END;
GO

