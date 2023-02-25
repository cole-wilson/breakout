import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import javax.swing.*;
import javax.sound.sampled.*;
import java.io.File;
import java.io.FileWriter;
import java.util.Scanner;

/***
 * Breakout.java
 *
 * A java implementation of Atari Breakout.
 * @author Cole Wilson
 * @date 6/9/2022
 */

public class Breakout extends JPanel {

    private int tick = 0;
    private Clip background_music = null;
    private int paddleX = 0;
    private double ballX, ballY;
    private double ball_speed, ball_angle;
    public static final int BALL_RADIUS = 5;
    private MODE mode = MODE.ATTRACT;
    private MODE nextmode = MODE.PLAY;
    private static final String high_score_file = System.getProperty("user.home") + File.separator + ".breakout_high_score.txt";
    private int score = 0;
    private Block[][] blocks = new Block[8][8];
    private int highscore = getHighScore();

    // https://www.w3schools.com/java/java_files_create.asp
    public static void setHighScore(int s) {
        try {
            FileWriter writer = new FileWriter(high_score_file);
            writer.write(""+s);
            writer.close();
        } catch (java.io.IOException e) {System.err.println(e);}
    }
    public static int getHighScore() {
        try {
            File f = new File(high_score_file);
            Scanner reader = new Scanner(f);
            int s = reader.nextInt();
            reader.close();
            return s;
        } catch (java.io.IOException e) {return 0;}
    }

    public Clip playSound(String file) { // https://stackoverflow.com/questions/15526255/best-way-to-get-sound-on-button-press-for-a-java-calculator
        try {
            AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(new File(file).getAbsoluteFile());
            Clip clip = AudioSystem.getClip();
            clip.open(audioInputStream);
            clip.addLineListener(new LineListener(){
                public void update(LineEvent event){
                    if(event.getType() == LineEvent.Type.STOP){
                        event.getLine().close();
                    }
                }
            });
            clip.start();
            return clip;
        }
        catch (UnsupportedAudioFileException | LineUnavailableException | java.io.IOException e) {System.err.println(e);}
        return null;
    }

    public void paintComponent(Graphics g) {
        Font mainFont;
        try {
            mainFont = Font.createFont(Font.TRUETYPE_FONT, new File("pixeloid_sans_bold.ttf"));
        } catch (java.io.IOException | FontFormatException e) {
            System.err.println(e);
            mainFont = new Font("monospaced", 1, 26);
        }
        g.setFont(mainFont.deriveFont(Font.BOLD, 18));

        super.paintComponent(g);

        if (mode == MODE.PLAY && background_music.isActive())
            background_music.stop();
        else if (mode != MODE.PLAY && !background_music.isActive())
            music();
        g.setColor(Color.WHITE);
        String info = "HIGH-SCORE: "+highscore+"    ";
        info += "SCORE: "+score;
        info += "    ANGLE:"+((int)ball_angle)+"    VELOCITY:"+(Math.round(ball_speed*100.0)/100.0);
        g.drawString(info, 2, 16);


        if (mode != MODE.DEAD) {
            // calculate paddle dimensions
            int paddle_width = getWidth()/5;

            // https://stackoverflow.com/questions/1439022/get-mouse-position
            Point screenPos = MouseInfo.getPointerInfo().getLocation();
            int panelX = screenPos.x - this.getLocationOnScreen().x;
            if (mode == MODE.PLAY && panelX >= 10 && panelX <= getWidth() - 10)
                paddleX = panelX - paddle_width/2;

            if (mode == MODE.ATTRACT)
                paddleX = (int)ballX - paddle_width/2 + (int)(Math.sin(tick/100.0) * (paddle_width/2 - 10));

            // draw paddle
            g.setColor(Color.WHITE);
            g.fillRect(paddleX, getHeight() - 10, paddle_width, 10);
            g.setColor(Color.GRAY);
            g.drawArc(paddleX, getHeight() - 8, paddle_width, 20, 0, 180);
            g.setColor(Color.RED);
            g.drawLine(paddleX + paddle_width/2, getHeight()-10, paddleX + paddle_width/2, getHeight());


            // draw blocks
            for (int y=0;y<this.blocks.length;y++) {
                for (int x=0;x<this.blocks[0].length;x++) {
                    this.blocks[y][x].draw(g, getWidth(), getHeight());
                }
            }

            double ball_change_x, ball_change_y;
            if (ball_angle == 90) {
                ball_change_x = 0;
                ball_change_y = ball_speed;
            }
            else if (ball_angle == -90) {
                ball_change_x = 0;
                ball_change_y =-ball_speed;
            }
            else {
                double m = Math.tan(Math.toRadians(90-ball_angle));
                double b = (double)ball_speed / Math.sqrt(Math.pow(m, 2) + 1);
                double a = b*m;
                ball_change_x = a;
                ball_change_y = b;
                // if (Math.abs(ball_angle) > 90) ball_change_x *= -1;
                if (ball_angle < 0) ball_change_y *= -1;
            }

            // if the ball hits either of the two "walls"
            if (ballX + ball_change_x + 2*BALL_RADIUS > getWidth() || ballX + ball_change_x < 0) {
                ball_change_x *= -1;
                ball_angle = Math.signum(ball_angle) * (180 - Math.abs(ball_angle));
                playSound("bounce.wav");
            }

            // if the ball hits the "ceiling"
            if (ballY < 0) {
                ball_change_y *= -1;
                ball_angle = -Math.signum(ball_angle) * (180 - Math.abs(ball_angle));
                playSound("bounce.wav");
            }

            // if the ball hits the "floor" or the paddle
            if (ballY + 2*BALL_RADIUS >= getHeight() - 10) {
                // the ball has hit the paddle
                if (ballX+BALL_RADIUS >= paddleX && ballX+BALL_RADIUS <= paddleX + paddle_width) {
                    double x_on_paddle = ballX - paddleX;
                    ball_angle = 140 - (int)((x_on_paddle/paddle_width) * 100);
                    ball_change_y *= -1;
                    // ball_angle = -ball_angle;
                    playSound("bounce.wav");
                }

                // the ball has NOT hit the paddle
                else {
                    playSound("end.wav");
                    mode = MODE.DEAD;
                    nextmode = MODE.PLAY;
                    if (score > highscore) {
                        for (int i=0;i<100;i++) playSound("score.wav");
                        setHighScore(score);
                        highscore = score;
                    }
                }
            }

            boolean isdone = true;
            for (int y=0;y<blocks.length;y++) {
                for (int x=0;x<blocks[0].length;x++) {
                    int hit = blocks[y][x].checkHit((int)ballX, (int)ballY, BALL_RADIUS);
                    if (hit == 1) {
                        ball_change_y *= -1;
                        ball_angle = -Math.signum(ball_angle) * (180 - Math.abs(ball_angle));
                    }
                    else if (hit == 2) {
                        ball_change_x *= -1;
                        ball_angle = Math.signum(ball_angle) * (180 - Math.abs(ball_angle));
                    }
                    if (hit != 0) {break;}
                    if (!blocks[y][x].isDone()) isdone = false;
                }
            }
            if (isdone) {
                if (mode == MODE.ATTRACT) {setup(); mode = MODE.ATTRACT;}
            }


            if (mode == MODE.PAUSED) {
                ball_change_x = 0;
                ball_change_y = 0;
            }
            // move and draw ball
            ballX += ball_change_x;
            ballY -= ball_change_y;
            g.setColor(Color.WHITE);
            g.fillOval((int)ballX, (int)ballY, 2*BALL_RADIUS, 2*BALL_RADIUS);
        }

        // Show Title Screen
        if (mode == MODE.ATTRACT || mode == MODE.DEAD) {
            g.setColor(new Color(127, 127, 127, 150));
            int top = (getHeight() - 300) / 2;
            g.fillRect((getWidth()-400)/2, top, 400, 300);
            g.setColor(Color.WHITE);
            g.setFont(mainFont.deriveFont(Font.BOLD, 60));
            if (mode == MODE.ATTRACT) {
                g.drawString("BlueJ", (getWidth() - 190)/2, top+100);
                g.drawString("Breakout", (getWidth() - 340)/2, top+150);
            }
            else if (mode == MODE.DEAD) {
                g.setColor(Color.RED);
                g.drawString("GAME", (getWidth() - 170)/2, top+100);
                g.drawString("OVER", (getWidth() - 160)/2, top+150);
            }
            if ((int)(tick/40) %2 == 0) {
                g.setColor(Color.WHITE);
                g.setFont(mainFont.deriveFont(Font.BOLD, 20));
                g.drawString("PRESS SPACE", (getWidth()-160)/2, top+200);
            }
            g.setColor(Color.WHITE);
            g.setFont(mainFont.deriveFont(Font.BOLD, 10));
            g.drawString("Move the mouse to move the paddle", (getWidth()-220)/2, top+240);
            g.drawString(" Break as many blocks as you can ", (getWidth()-220)/2, top+250);
            g.drawString("  Higher blocks are more points  ", (getWidth()-220)/2, top+260);
            g.drawString("     ...but harder to break      ", (getWidth()-220)/2, top+270);
            g.drawString("    It gets faster as you go!    ", (getWidth()-220)/2, top+280);
        }

        // keep angle between -180 and 180
        ball_angle %= 180;
    }


    public void setup() {
        mode = MODE.PLAY;
        playSound("bounce.wav");
        playSound("start.wav");

            score = 0;
            ball_angle = 90 + (Math.random() - 0.5);
            ball_speed = 1;
            ballX = 245;
            if (getHeight() == 0)
                ballY = 400 - 2*BALL_RADIUS;
            else
                ballY = getHeight() - 90 - 2*BALL_RADIUS;

            for (int y=0;y<blocks.length;y++) {
                for (int x=0;x<blocks[0].length;x++) {
                    blocks[y][x] = new Block(x, y, blocks.length - y);
                }
            }
    }
    public void music() {
        this.background_music = playSound("music.wav");
        background_music.loop(background_music.LOOP_CONTINUOUSLY);
    }
    public Breakout() {
        setup();
        music();
        addKeyListener(new CustomKeyListener());
        setFocusable(true);

        Timer t = new Timer(10, new CustomActionListener());
        t.setRepeats(true);
        t.setInitialDelay(0);
        t.start();

        this.mode = MODE.ATTRACT;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
                var panel = new Breakout();
                panel.setBackground(Color.BLACK);
                var frame = new JFrame("BlueJ Breakout");
                frame.setSize(500, 500);
                frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                frame.getContentPane().add(panel, BorderLayout.CENTER);
                // https://stackoverflow.com/questions/11570356/jframe-in-full-screen-java
                frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
                frame.setUndecorated(true);
                frame.setVisible(true);

                // https://stackoverflow.com/questions/1984071/how-to-hide-cursor-in-a-swing-application
                BufferedImage cursorImg = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
                Cursor blankCursor = Toolkit.getDefaultToolkit().createCustomCursor(
                cursorImg, new Point(0, 0), "blank cursor");
                frame.getContentPane().setCursor(blankCursor);
            });
    }

    public class CustomActionListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            Breakout.this.tick++;
            Breakout.this.repaint();
        }
    }


    public class CustomKeyListener implements KeyListener {
    // info from http://www.edu4java.com/en/game/game4.html

        public void keyTyped(KeyEvent e) {}
        public void keyPressed(KeyEvent e) {}
        public void keyReleased(KeyEvent e) {
            System.out.println(e.getKeyCode());
            switch (e.getKeyCode()) {
                case 32:
                    if (Breakout.this.nextmode == MODE.PLAY && Breakout.this.mode != MODE.PAUSED)
                        Breakout.this.setup();
                    if (Breakout.this.nextmode != null) {
                        Breakout.this.mode = Breakout.this.nextmode;
                        if (Breakout.this.mode == MODE.PLAY)
                            Breakout.this.nextmode = MODE.PAUSED;
                            else  if (Breakout.this.mode == MODE.PAUSED)
                            Breakout.this.nextmode = MODE.PLAY;
                            else
                            Breakout.this.nextmode = null;
                    }

                    break;
                // case 82:
                    // Breakout.this.setup();
                    // break;
                case KeyEvent.VK_ESCAPE:
                    Breakout.this.setup();
                    Breakout.this.mode = MODE.ATTRACT;
                    Breakout.this.nextmode = MODE.PLAY;
                    break;
            }
            Breakout.this.repaint();
        }
    }
    public class Block {
        private Color[] rowColors = {
            Color.BLACK,
            new Color(148,0,211), // violet
            new Color(75,0,130), // indigo
            Color.BLUE,
            Color.CYAN,
            Color.GREEN,
            Color.YELLOW,
            new Color(255, 127, 0), // orange
            Color.RED
        };
        private int x, y, color, points;
        private boolean hitlast = false;
        private int screenX, screenY, screenW, screenH;
        public Block(int col, int row, int strength) {
            this.x=col;
            this.y=row;
            this.color = strength;
            this.points = strength;
        }
        public void draw(Graphics g, int width, int height) {
            final int SPACING = 5;
            final int blocks_per_row = Breakout.this.blocks[0].length;
            final int rows = Breakout.this.blocks.length;
            this.screenW = (getWidth() - (SPACING*blocks_per_row))/blocks_per_row;
            this.screenH = (2*((getHeight()) / 3) - (SPACING*rows) - 20)/rows;
            this.screenX = ((SPACING+this.screenW)*x + SPACING/2);
            this.screenY =  20 + (SPACING+this.screenH)*y;

            g.setColor(this.rowColors[color]);
            g.fillRect(
                    this.screenX,
                    this.screenY,
                    this.screenW,
                    this.screenH
            );
        }
        public int checkHit(int ballX, int ballY, int radius) {
            if (this.color == 0 || this.hitlast) {hitlast=false;return 0;}

            if (ballX+2*radius>=screenX&&ballX<=screenX+screenW&&ballY+2*radius>=screenY&&ballY<=screenY+screenH) {
                color--;
                if (color == 0) {
                    Breakout.this.score += this.points;
                    Breakout.this.ball_speed += 0.2;
                    Breakout.this.playSound("score.wav");
                } else {
                    Breakout.this.playSound("bounce.wav");
                }
                hitlast = true;
                if (ballX+2*radius >= screenX && ballX+2*radius <= screenX) return 2;
                if (ballX >= screenX+screenW  && ballX <= screenX+ screenW) return 2;
                return 1;
            }
            else {
                return 0;
            }
        }
        public boolean isDone() {return this.color == 0;}
    }
}
enum MODE {
    ATTRACT,
    SETTINGS,
    PLAY,
    DEAD,
    PAUSED,
}
