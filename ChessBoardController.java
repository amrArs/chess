import java.beans.beancontext.BeanContext;
import java.io.IOException;
import java.nio.channels.SeekableByteChannel;
import java.nio.channels.SelectableChannel;
import java.text.BreakIterator;
import java.util.ArrayList;
import java.util.Currency;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import javax.swing.ViewportLayout;
import javax.swing.plaf.TextUI;
import javax.swing.plaf.synth.SynthOptionPaneUI;
import javax.swing.plaf.synth.SynthScrollBarUI;
import javax.swing.text.html.HTMLDocument.RunElement;

import javafx.css.ParsedValue;
import javafx.css.SimpleStyleableDoubleProperty;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.PickResult;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

@Controller
public class ChessBoardController {

    @FXML GridPane chessBoard;

    StackPane selectedPieceContainer;
    boolean alreadySelectedSquare = false;
    public static ImageView selectedPiece;
    public static StackPane selectedSquare;

    
    // Global variables 
    char columnCounter = ' ';
    int rowCounter = 0;

    String selectedPieceCatg;
    public static String selectedPieceTeam;
    String otherPieceTeam;

    boolean pieceHasBeenKilled;

    boolean checkIsCheckmate = false;
    boolean isCheckmate = false;
    boolean pieceInTheWay = false;
    String threatPosition;
    ImageView threatPiece;
    
    boolean isWhiteTeamTurn = true;

    
    List<String> kingProtectionSpots = new ArrayList<String>();
    
    public static StackPane currentPawnSquare;
    public static ImageView currentPawnPiece;
    public static Stage pawnRolePanel = new Stage();
    


    
    public void move_pieces(Event event) throws IOException {
        // The main function. Games starts from here 

        selectedSquare = (StackPane) event.getSource(); // Convert from Event object to StackPane object

        pieceHasBeenKilled = false; // No piece is killed by default

        disSelection(); // diSelection all the squares

        if (selectedSquare.getChildren().size() > 0) {
            // This block of code will be executed if only the square has a piece inside it.
            
            selectedSquare.getStyleClass().add("selected"); // Select the square that has a piece
            
            kill(); // kill the opponnet
            
            selectedPiece = (ImageView) selectedSquare.getChildren().get(0);
            selectedPieceCatg = selectedPiece.getStyleClass().get(1);
            selectedPieceTeam = selectedPiece.getStyleClass().get(2);

            
            alreadySelectedSquare = true;
            
            clear_valid_moves(); // to start over again
            
            
            if(!is_team_turn(selectedPieceTeam)) return; // if it's not the team's turn. then break the whole operation and don't select over the piece.

            if (pieceHasBeenKilled == false) valid_moves(); // When you kill the enmey. it will invoke the `valid_moves` function, which will lead to show the valid moves again (although we already moved). To avoid this. we have the variable `pieceHasBeenKilled`

        } else if (selectedSquare.getChildren().size() == 0 &&
         alreadySelectedSquare &&
         selectedSquare.getStyleClass().contains("valid_move")) {

            set_pawn_alreadyMoved_class(); // If it's a pawn, tells the program he moved so he no longer has 2 moves 

            selectedSquare.getChildren().add(selectedPiece); // Add the new piece to the new square
            alreadySelectedSquare = false; // reset to false, to start over again
            show_pawn_role_panel(); // in case the pawn reached the end of the chess board

            next_turn(selectedPieceTeam); // Change the turn team

            // clear Every thing and start over again
            disSelection();
            clear_valid_moves();
            
            clear_checkmate();
            clear_possible_kill_for_king();
            clear_king_protection_spots();
            
            check_checkmate();

            checkIsCheckmate = false; 
            
        }

    }  

    public void kill() throws IOException {

        // If the player clicked on a piece, The program checks if The player wants to select over a new square
        // or if the player wants to kill the opponnet piece
        // if the `alreadySelectedSquare` variable is set to `true`, it means that the player already selected over a square
        // So he wants to kill the opponnet piece. Not to select.

        // The piece that player wants to kill should has the class `valid_move`

        // And of course, it should be the team turn, this is `next_turn()` function job
        // which determind what team's turn

        if (alreadySelectedSquare && selectedSquare.getStyleClass().contains("valid_move")) {

            next_turn(selectedPieceTeam);
            
            clear_possible_kill_for_king(); // reset the the values

            // When the player kills the opponnet piece, we delete the opponent piece from the required square, 
            // and then add ours

            selectedSquare.getChildren().remove(0); // delete the opponnet piece.
            selectedSquare.getChildren().add(selectedPiece); // add our piece
            
            show_pawn_role_panel(); // this function checks if the pawn reached the end of the Chess board

            alreadySelectedSquare = false; // Reset the the value to start over again

            disSelection(); // diselection all squares to start over again
            clear_valid_moves(); // clear all selected squares to start over again

            pieceHasBeenKilled = true; // set it to true, we will need it later

            clear_checkmate(); // clear if there is a checkmate To start over again
            clear_possible_kill_for_king(); // clear all the spot that may king may get killed, To start over again
            check_checkmate(); // Check if the killed piece cause to CheckMate
            checkIsCheckmate = false; // Not important now

            
            
            
        }
    }

    public void disSelection() {
        // Loop through every square and remove the `selected` class from it.
        for (Node currentSquare: chessBoard.getChildren()) {
            currentSquare.getStyleClass().remove("selected");
        }
    }
    
    public void valid_moves() {

        // This function will do the following:
        // 1- define the colum/row of the selected piece
        // 2- define the selected piece category, king, knight, pawn, etc...
        // 3- invoke the required function according to the piece category;

        char column = selectedSquare.getStyleClass().get(0).charAt(0); 
        char row = selectedSquare.getStyleClass().get(0).charAt(1);
        // we used the `charAt` function because the variable type is `char`.

        String selectedPieceCatg = selectedSquare.getChildren().get(0).getStyleClass().get(1);

        switch (selectedPieceCatg) { // `selectedPieceCatg` variable has the piece category.
            case "knight":
                knight_movement(column, row);
                break;
        
            case "rook":
                vertical_movement(column, row);
                horizontal_movement(column, row);
                break;

            case "bishop":
                side_walk_movement(column, row); 
                break;

            case "queen":
                vertical_movement(column, row);
                horizontal_movement(column, row);
                side_walk_movement(column, row);
                break;

            case "king": 
                king_movement(column, row);
                break;

            case "pawn": 
                pawn_movement(column, row);
                break;
            // default:
            //     break;
        }
        
    }
   
    public void clear_valid_moves() {
        // Clear valid moves class from all squares after moving a piece
        for (Node currentSquare: chessBoard.getChildren()) {
            currentSquare.getStyleClass().remove("valid_move");
        }
    }

    public void knight_movement(char column, char row) {

        // All the possible moves for the knight, they are in a `char[]` array.
        char [][] validPositions = { 
            {(char) (column - 1), (char) (row - 2)},
            {(char) (column - 2), (char) (row - 1)},
            {(char) (column - 2), (char) (row + 1)},
            {(char) (column - 1), (char) (row + 2)},
            {(char) (column + 1), (char) (row + 2)},
            {(char) (column + 2), (char) (row + 1)},
            {(char) (column + 2), (char) (row - 1)},
            {(char) (column + 1), (char) (row - 2)},
        }; 
        
        // invoke the function to define what square is considerd a valid move among the `validPositions`
        king_and_knight_set_valid_position(validPositions); 
        
    }

    public void king_movement(char column, char row) {

        // All the possible moves for the king, they are in a `char[]` array.
        char [][] validPositions = { 
            {(char) (column), (char) (row - 1)},
            {(char) (column + 1), (char) (row - 1)},
            {(char) (column + 1), (char) (row)},
            {(char) (column + 1), (char) (row + 1)},

            {(char) (column), (char) (row + 1)},
            {(char) (column - 1), (char) (row + 1)},
            {(char) (column - 1), (char) (row)},
            {(char) (column - 1), (char) (row - 1)},
        }; 

        // invoke the function to define what square is considerd a valid move among the `validPositions`
        king_and_knight_set_valid_position(validPositions);

    }

    public void king_and_knight_set_valid_position(char[][] validPositions) {
        // king and knight have similar moves, so they were put in the same function

        for (Node currentSquare: chessBoard.getChildren()) {
            // loop through all the squares
            for (char[] checkValidPosition: validPositions) {
                // loop through the validPositions` array,
                // which contains the possible moves for king/knight ()
                
                if (((StackPane) currentSquare).getChildren().size() > 0) {

                    
                    selectedPieceTeam = selectedPiece.getStyleClass().get(2);
                    otherPieceTeam = ((StackPane) currentSquare).getChildren().get(0).getStyleClass().get(2);
                    
                    // if the piece is in our team, no need to continue
                    if (selectedPieceTeam.equals(otherPieceTeam) && checkIsCheckmate == false) break;
                    
                }


                // if the current square (we are looping through) is a valid move for king/knight:
                if (currentSquare.getStyleClass().get(0).equals(String.valueOf(checkValidPosition)) ) {

                    set_safe_spot(currentSquare); 
                    // set the class `safe_spot_for_white/black`
                    // this class will be used later to decide if the spot safe or not for the king

                    if (selectedPieceCatg.equals("king")) safe_spot_for_king(currentSquare);
                    // if it's a king (not knight) then the program checks if it's safe spot or not.

                    // if it's a knight:
                    else {
                        
                        // checks if the king in danger, then add the `valid_move` class
                        // if (!selectedPieceCatg.equals("king")) set_checkmate_class(currentSquare); // set the checkmate class in case it's checkmate
                        set_checkmate_class(currentSquare);
                        if (is_king_in_danger(String.valueOf(checkValidPosition))) set_valid_move_class(currentSquare);
                        
                    }
                    

                    
                    
                }
            }
        }
    }

    public void safe_spot_for_king(Node currentSquare) {

        if (selectedPieceTeam.equals("white") && !currentSquare.getStyleClass().contains("safe_spot_for_black")) set_valid_move_class(currentSquare);
        else if (selectedPieceTeam.equals("black") && !currentSquare.getStyleClass().contains("safe_spot_for_white")) set_valid_move_class(currentSquare);
    }

    public void vertical_movement(char column, char row) {
        // define the selected piece position, column/row
        columnCounter = column;
        rowCounter = Character.getNumericValue(row); 

        // From down to up
        for (; rowCounter >= 1; rowCounter--) {
          
            if (set_validPosition()) break; // the opeartion will be ended when we return `pieceInTheWay=true`
            

        }

        // reset the values to start over again
        columnCounter = column;
        rowCounter = Character.getNumericValue(row);


        // from up to down
        for (; rowCounter <= 8; rowCounter++) {
          
            if (set_validPosition()) break; // the opeartion will be ended when we return `pieceInTheWay=true`

        }
    }

    public void horizontal_movement(char column, char row) {

        // define the selected piece position, column/row
        columnCounter = column;
        rowCounter = Character.getNumericValue(row);

        for (; columnCounter >= 'a'; columnCounter--) {

            if (set_validPosition()) break; // the opeartion will be ended when we return `pieceInTheWay=true`

        }

        // reset the values to start over again
        columnCounter = column;
        rowCounter = Character.getNumericValue(row);

        for (; columnCounter <= 'h'; columnCounter++) {
          
            if (set_validPosition()) break; // the opeartion will be ended when we return `pieceInTheWay=true`

        }

    }

    public void side_walk_movement(char column, char row) {
        // Right Side Movement

        // Reset values to Start over again
        columnCounter = column;
        rowCounter = Character.getNumericValue(row);
        
        for (; rowCounter >= 1; columnCounter++, rowCounter--) {
            
            if (set_validPosition()) break;

        }

        // Reset values to Start over again
        columnCounter = column;
        rowCounter = Character.getNumericValue(row);

        for (; rowCounter <= 8; columnCounter++, rowCounter++) {
            
            if (set_validPosition()) break; // the opeartion will be ended when we return `pieceInTheWay=true`

        }


        // Left sideWalk movement
        columnCounter = column;
        rowCounter = Character.getNumericValue(row);
        
        for (; rowCounter >= 1; columnCounter--, rowCounter--) {
            
            if (set_validPosition()) break;

        }

        // Reset values to Start again
        columnCounter = column;
        rowCounter = Character.getNumericValue(row);

        for (; rowCounter <= 8; columnCounter--, rowCounter++) {
            
            if (set_validPosition()) break;

        }
    }

    public boolean set_validPosition() {

        pieceInTheWay = false; // by default it's set to false

        for (Node currentSquare: chessBoard.getChildren()) {

            priority_to_king();
            
            if (currentSquare.getStyleClass().contains(columnCounter + "" + rowCounter) &&
                !currentSquare.equals(selectedSquare)) {

                    // Enter when only the square has a piece
                    if (((StackPane) currentSquare).getChildren().size() > 0) {
                        
                        String currentPieceCatg = ((StackPane) currentSquare).getChildren().get(0).getStyleClass().get(1);

                        
                        
                        set_checkmate_class(currentSquare); // set the checkmate class in case it's checkmate
                        
                        if (!is_enemy(currentSquare) && checkIsCheckmate == false) break; // Break the operation if it's not enemy

                        // else, if it's an enemy and king, we should not break the operation.
                        // and the we tell the program there is no piece in the way ()`pieceInTheWay=false`).
                        else if (checkIsCheckmate && is_enemy(currentSquare) && currentPieceCatg.equals("king")) pieceInTheWay = false;
                    
                        
                        
                    }
                    
                    // If king in danger (checkmate), the program checks first wheather the current square is meant to protect the king.
                    // if it's not, then we return the value `false`. which means the `valid_move` class will not be added.

                    // if the king not in danger. then continue and add the `valid_move` class normally.
                    if (is_king_in_danger(columnCounter + "" + rowCounter)) set_valid_move_class(currentSquare);
                    
                    
                    set_safe_spot(currentSquare); // set the current square with the `safe_spot_for__`. will be used later.
                    
                    
            }
        }

        return pieceInTheWay; 
        // Finally we return if there is a piece in the way or not. to break the operation
        // the resutl will be returned to the `valid_move()` function
    }


    public boolean is_king_in_danger(String currentPosition) {

        boolean status = true;

        // if the king not in danger (checkmate) then return `true`. 
        // if not:
        if (isCheckmate) {
            for (String kingProtectionSpot: kingProtectionSpots) {
                // if the king is in danger. then the pieces can't move free
                // they should only move in order to protect the king
                // the `kingProtectionSpots` contains all the squares that may save the king from getting killed
                // if the current square position not in `kingProtectionSpots`. the the piece can't move to it

                if (currentPosition.equals(kingProtectionSpot)) {
                    status = true; // if the square in the `kingProtectionSpots`, then the piece can move to it (in order to portect the king)
                    break; // no need to continue 
                }   
                else {
                    status = false; // if the current square not in the `kingProtectionSpots`. the piece can not move to it.
                }
            }
        } 

        
        
        return status; // return the status wheather it can svae the king or not (true/false)
    }


    public boolean is_enemy(Node currentSquare) {

        boolean isEnemy = false; // by default it's not an enemy.
        if (((StackPane) currentSquare).getChildren().size() > 0) {

            pieceInTheWay = true; // the program should know if there is a piece in the way, then define if it's enemy or not

            String selectedPieceTeam = selectedPiece.getStyleClass().get(2);
            String otherPieceTeam = ((StackPane) currentSquare).getChildren().get(0).getStyleClass().get(2);

            if (!selectedPieceTeam.equals(otherPieceTeam)) isEnemy = true; // if its color not like us, then it's an enemy
        }

        return isEnemy; // return the status wheather it's an enemy or not (turn/false)
    }

    public void check_checkmate() {
        

        // This Function will be executed ONLY after we move a piece 
        // after we move the piece, it will checks if this moves cause a checkmate

        checkIsCheckmate = true; // we will need it later

        for (Node currentSquare: chessBoard.getChildren()) {

            if (((StackPane) currentSquare).getChildren().size() > 0) {
                String selectedPieceCatg = ((StackPane) currentSquare).getChildren().get(0).getStyleClass().get(1);

                char column = currentSquare.getStyleClass().get(0).charAt(0);
                char row = currentSquare.getStyleClass().get(0).charAt(1);

                selectedSquare = (StackPane) currentSquare;
                selectedPiece = (ImageView) ((StackPane) currentSquare).getChildren().get(0);
                selectedPieceTeam = selectedPiece.getStyleClass().get(2);

                switch (selectedPieceCatg) {
                    case "knight":
                        knight_movement(column, row);
                        break;
                
                    case "rook":
                        vertical_movement(column, row);
                        horizontal_movement(column, row);
                        break;
        
                    case "bishop":
                        side_walk_movement(column, row); 
                        break;
        
                    case "queen":
                        vertical_movement(column, row);
                        horizontal_movement(column, row);
                        side_walk_movement(column, row);
                        break;
        
                    case "king": 
                        king_movement(column, row);
                        break;
        
                    case "pawn": 
                        pawn_movement(column, row);
                        break;
                }
            }
        }
    
    }

    public void priority_to_king() {

        // In case of checkmate, This Function will do the following:

        // Define the threat position, and the king position

        String kingPosition = "";
        char kingPositionColumn = ' ';
        char kingPositionRow = ' ';
        if (isCheckmate) {

            char threatPositionColumn = threatPosition.charAt(0);
            char threatPositionRow = threatPosition.charAt(1);
            // outer:
            for (Node currentSuqaure: chessBoard.getChildren()) {
                if (currentSuqaure.getStyleClass().contains("checkmate")) {

                    kingPosition = currentSuqaure.getStyleClass().get(0);
                    kingPositionColumn = kingPosition.charAt(0);
                    kingPositionRow = kingPosition.charAt(1);

                    
                }
            }



            // Start Horizntal
            if (kingPositionRow == threatPositionRow) {
                for (; threatPositionColumn > kingPositionColumn;) {
                    add_to_king_protection_spots(threatPositionColumn + "" + threatPositionRow);
                    threatPositionColumn--;

                }

                for (; threatPositionColumn < kingPositionColumn;) {
                    add_to_king_protection_spots(threatPositionColumn + "" + threatPositionRow);
                    threatPositionColumn++;

                }

                
            }

            // Start Vertical
            else if (threatPositionColumn == kingPositionColumn) {

                for (; threatPositionRow > kingPositionRow;) {
                    add_to_king_protection_spots(threatPositionColumn + "" + threatPositionRow);
                    threatPositionRow--;

                }
            
                for (; threatPositionRow < kingPositionRow;) {
                    add_to_king_protection_spots(threatPositionColumn + "" + threatPositionRow);
                    threatPositionRow++;

                }

            }

            // Start Side movement
            else if (threatPositionColumn > kingPositionColumn &&
                    threatPositionRow > kingPositionRow) {
                        for (; threatPositionColumn > kingPositionColumn;) {

                            add_to_king_protection_spots(threatPositionColumn + "" + threatPositionRow);
                            threatPositionColumn--;
                            threatPositionRow--;
        
                        }
            }

            else if (threatPositionColumn < kingPositionColumn &&
                    threatPositionRow < kingPositionRow) {
                        for (; threatPositionColumn < kingPositionColumn;) {

                            add_to_king_protection_spots(threatPositionColumn + "" + threatPositionRow);
                            threatPositionColumn++;
                            threatPositionRow++;
        
                        }
            }

            else if (threatPositionColumn > kingPositionColumn &&
                    threatPositionRow < kingPositionRow) {
                        for (; threatPositionColumn > kingPositionColumn;) {

                            add_to_king_protection_spots(threatPositionColumn + "" + threatPositionRow);
                            threatPositionColumn--;
                            threatPositionRow++;
        
                        }
            }

            else if (threatPositionColumn < kingPositionColumn &&
                    threatPositionRow > kingPositionRow) {
                        for (; threatPositionColumn < kingPositionColumn;) {

                            add_to_king_protection_spots(threatPositionColumn + "" + threatPositionRow);
                            threatPositionColumn++;
                            threatPositionRow--;
        
                        }
            }

            // End Side movement
        }
    }

    public void pawn_movement(char column, char row) {

        rowCounter = Character.getNumericValue(row);
        String pawnTeam = selectedPiece.getStyleClass().get(2);  
        
        switch (pawnTeam) {
            // The white pawn should be walking up
            case "white":
                row--;
                rowCounter--;
                break;
        
            // The black pawn should be walking down
            default:
                row++;
                rowCounter++;
                break;

        }

        pawn_set_valid_position(column, row, pawnTeam); // Invoke the the function that will set the `valid_move` class

        
    }

    public void pawn_set_valid_position(char column, char row, String pawnTeam) {
        pieceInTheWay = false;

        // This array contains the possible moves for the pawn WHEN HE KILLS
        char [][] validPositions = {
            {(char) (column - 1), (char) (row)},
            {(char) (column + 1), (char) (row)},
        };

        
           
        // This Loop for normal moves
        for (Node currentSquare: chessBoard.getChildren()) {
            if (currentSquare.getStyleClass().get(0).equals(column + String.valueOf(rowCounter))) {
                
                if (((StackPane) currentSquare).getChildren().size() > 0 && !currentSquare.equals(selectedSquare)) break;

                if (is_king_in_danger(column + String.valueOf(rowCounter))) set_valid_move_class(currentSquare);

                if (!selectedPiece.getStyleClass().contains("alreadyMoved") && 
                    pawnTeam.equals("white")){
                        rowCounter--;
                        set_pawn_first_move(column);
                        break;
                }

                if (!selectedPiece.getStyleClass().contains("alreadyMoved") && 
                    pawnTeam.equals("black")){
                        rowCounter++;
                        set_pawn_first_move(column);
                        break;
                }
            }
        }
        
        
        // This loop for kill the enemy
        for (Node currentSquare: chessBoard.getChildren()) {
            for (char[] checkValidPosition: validPositions) {
                String stringCheckValidPosition = String.valueOf(checkValidPosition);

                if (currentSquare.getStyleClass().get(0).equals(stringCheckValidPosition) &&
                !currentSquare.getStyleClass().contains("valid_move")) {
                    
                    set_safe_spot(currentSquare); 
                    
                    if (((StackPane) currentSquare).getChildren().size() > 0) {
                           
                        if (!is_enemy(currentSquare) && checkIsCheckmate == false) break;
                        
                        set_checkmate_class(currentSquare);
                        if (is_king_in_danger(stringCheckValidPosition)) set_valid_move_class(currentSquare);
                        
                    }
                }
            }
        }
    }

    public void set_pawn_alreadyMoved_class() {
        if (selectedPieceCatg.equals("pawn")) selectedPiece.getStyleClass().add("alreadyMoved");
        // The pawn no longer has 2 moves when we set the class `alreadyMoved`
    }


    public void show_pawn_role_panel() throws IOException {

        // Create a new Stage for the Pawn Change Role Panel
        Parent root = FXMLLoader.load(getClass().getResource("PawnRolePanel.fxml"));
        Scene scene = new Scene(root);
        pawnRolePanel.setScene(scene);
        
        if (selectedPieceCatg.equals("pawn")) {
            
            // After we check if it's pwan, we stores it in a variable to use it later. (currentPawnSquare/currentPawnPiece)
            currentPawnSquare = selectedSquare;
            currentPawnPiece = selectedPiece;

            if (selectedPieceTeam.equals("white") &&
                rowCounter == 1) pawnRolePanel.show(); // Show the panel when it reached to the end of the chess board (White).

            else if (selectedPieceTeam.equals("black") &&
                    rowCounter == 8) pawnRolePanel.show(); // Show the panel when it reached to the end of the chess board (Black).
        }
    }

    public void select_from_pawn_role_panel(Event event) {

        // After the Panel has been displayed, The user should select a piece from it.

        // Define the required information
        ImageView newPiece = (ImageView) event.getSource(); // Convert from Event object to StackPane object
        String newPieceCatg = newPiece.getStyleClass().get(1);
        String currentPawnPieceTeam = currentPawnPiece.getStyleClass().get(2);

        Image path = new Image(currentPawnPieceTeam + "Pieces/" + newPieceCatg + ".png");
        // The piece that the player choosed, will be place in the previous pawn square/position

        currentPawnPiece.setImage(path);

        currentPawnPiece.getStyleClass().set(1, newPieceCatg); 
        // The new piece that the user selected, we should set its own category. (wheather he selected rook/knight/bishop/queen)
        
        pawnRolePanel.close(); // Close the panel after the user select a piece    
        
    }

    public void set_pawn_first_move(char column) {
            // This function responsible for the pawn first 2 steps

            for (Node currentSquare: chessBoard.getChildren()) {
                if (currentSquare.getStyleClass().contains(column + String.valueOf(rowCounter))) {
                    if (((StackPane) currentSquare).getChildren().size() > 0 && !currentSquare.equals(selectedSquare)) break;
                     // Break the opeartion if there is a piece in front of the pawn


                    if (is_king_in_danger(columnCounter + "" + rowCounter)) set_valid_move_class(currentSquare);
                    // if the king is not in danger. set the `valid_move` class
                }
        }
    }


    public void set_valid_move_class(Node currentSquare) {
        // This function will be executed ONLY when we click on a piece, not after we move it
        if (checkIsCheckmate == false) currentSquare.getStyleClass().add("valid_move");
    }

    public void set_safe_spot(Node currentSquare) {
        // Will only be executed after we move the piece 

        // define wheather the currentSquare is safe for the king
        if (checkIsCheckmate && !currentSquare.getStyleClass().contains("safe_spot_for_" + selectedPieceTeam)) {
            currentSquare.getStyleClass().add("safe_spot_for_" + selectedPieceTeam);
        }
    }

    public void clear_checkmate() {
        // Clear checkmate To start over again
        isCheckmate = false;
        for (Node currentSquare: chessBoard.getChildren()) {
            currentSquare.getStyleClass().remove("checkmate");
        }
    }

    public void clear_possible_kill_for_king() {
        // Clear the possible spots that the king may get killed, To start over again
        for (Node currentSquare: chessBoard.getChildren()) {
            currentSquare.getStyleClass().remove("safe_spot_for_black");
            currentSquare.getStyleClass().remove("safe_spot_for_white");
        }
    }


    public void add_to_king_protection_spots(String protectionSpot) {
        // The `protectionSpot` is the square between the threat and the king

        if (!kingProtectionSpots.contains(protectionSpot)) {
            kingProtectionSpots.add(protectionSpot); // add this square the `kingProtectionSpots`
        }
    }

    public void clear_king_protection_spots() {
        kingProtectionSpots.clear(); // deletet all the squares of the array, To start over again
    }

    public void set_checkmate_class(Node currentSquare) {
        if (((StackPane) currentSquare).getChildren().size() > 0) {

            String currentPieceCatg = ((StackPane) currentSquare).getChildren().get(0).getStyleClass().get(1);
    
            if (checkIsCheckmate && currentPieceCatg.equals("king") && is_enemy(currentSquare)) {
                currentSquare.getStyleClass().add("checkmate");
                isCheckmate = true;
                threatPiece = selectedPiece; // stores the piece that threat the king, will be used later
                threatPosition = selectedSquare.getStyleClass().get(0); // stores the piece position that threat the king, will be used later
            } 
        }
        
        // This function will only be executed after we move the piece

        // check if the mvoe caused a checkmate

    }


    public boolean is_team_turn(String selectedPieceTeam) {
        // Team's who turn to play
        boolean isTeamTurn = false;

        if (isWhiteTeamTurn && selectedPieceTeam.equals("white") || 
        !isWhiteTeamTurn && selectedPieceTeam.equals("black")) 
        isTeamTurn = true;

        return isTeamTurn;
    }

    public void next_turn(String selectedPieceTeam) {

        // After the white moves, it's the black turn
        // if (selectedPieceTeam.equals("white")) isWhiteTeamTurn = false;
        // else isWhiteTeamTurn = true;
    }
}