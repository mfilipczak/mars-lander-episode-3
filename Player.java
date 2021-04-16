import java.util.*;
import java.util.stream.Collectors;
import java.io.*;
import java.math.*;

/**
 * Auto-generated code below aims at helping you parse
 * the standard input according to the problem statement.
 **/
class Player {


    static int NUMBER_OF_LANDERS = 40;
    static int NUMBER_OF_POPULATIONS = 250;
    static double MIX_POPULATION_CHANCE = 0.01;
    static int REPRODUCING_LANDERS = 5;  // Per population
    static int MAX_TIMESTEP = 200;

    static int LANDERS_IN_POPULATION = (int)Math.floor(NUMBER_OF_LANDERS / NUMBER_OF_POPULATIONS);

    static class Point {
        double x;
        double y;
        double distance;

        public Point(double x, double y) {
            this.x = x;
            this.y = y;
        }

        public double distanceTo(Point o) {
            return Math.sqrt(Math.pow(o.x - x, 2) + Math.pow(o.y - y, 2));
        }

        public String toString() {
            return x +" " + y;
        }
    }

    static class Level {
        double g = 3.711;
        Point minFlat = null;
        Point maxFlat = null;
        int li;
        Point middle;
        double maxDistance = 2 * Math.sqrt(7000*7000+3000*3000);
        List<Point> points = new ArrayList<>();

        public void init() {
            middle = new Point(minFlat.x + (maxFlat.x - minFlat.y)/2, minFlat.y);
            calculateDistances();
            //   maxDistance = Math.max(middle.distanceTo(new Point(0,0)), middle.distanceTo(new Point(7000,0)));
        }
        void calculateDistances() {

            // Find the two points forming the landing area
            Point lp1 = minFlat; // First landing point
            Point lp2 = maxFlat; // Second landing point
              lp1.distance = 0;
            lp2.distance = 0;

            // Propagate distances away from the landing area
            for (int i = li + 2; i < this.points.size(); i++) {
                Point other = this.points.get(i-1);
                this.points.get(i).distance = other.distance;
                this.points.get(i).distance += this.points.get(i).distanceTo(other);
            }
            for (int i = li - 1; 0 <= i; i--) {
                Point other = this.points.get(i+1);
                this.points.get(i).distance = other.distance;
                this.points.get(i).distance += this.points.get(i).distanceTo(other);
            }
        }
        public double getDistanceToLandingArea(Point point) {
            double bestDistance = Double.MAX_VALUE;
            for (int i = 0; i < this.points.size(); i++) {
                Point p = this.points.get(i);
                double distance = p.distanceTo(point) + p.distance;
                if (bestDistance < distance) {
                    continue;
                }

                // Look for line intersections
                boolean found = false;
                for (int j = 1; j < this.points.size(); j++) {
                    if (j == i || j - 1 == i) {
                        continue;
                    }
                    Point p1 = this.points.get(j-1);
                    Point p2 = this.points.get(j);
                    if (lineIntersection(point, p, p1, p2)) {
                        found = true;
                        break;
                    }
                }
                if (found) {
                    continue;
                }
                bestDistance = distance;
            }
            return bestDistance;
        }
    }

    static class Population {
        String name;
        List<Lander> landers = new ArrayList<>();


        int getBestScore() {
            return landers.get(0).score;
        }

        void calculateScores() {
            final long sum = landers.stream().mapToInt(Lander::getScore).sum();
            for(int i = landers.size() - 1;i>=0;i--) {
                landers.get(i).cumulativeScore = (double)landers.get(i).score/sum;
                if(i<landers.size() - 1) {
                    landers.get(i).cumulativeScore+=landers.get(i+1).cumulativeScore;
                }
            }
        }
    }

    static class Command {
        int rotate;
        double power;

        public Command(int rotate, double power) {
            this.rotate = rotate;
            this.power = power;
        }

        public Command(Command c) {
            this.rotate = c.rotate;
            this.power = c.power;
        }

        @Override
        public String toString() {
            return "{" +
                    "r=" + rotate +
                    ",p=" + power +
                    '}';
        }

        public void copyCommand(Command command) {
            this.power = command.power;
            this.rotate = command.rotate;
        }
    }

    static class Lander {
        int initXSpeed;
        int initYSpeed;
        int initX;
        int initY;
        int initRotate;
        String name;
        double lastDiff;
        double cumulativeScore;

        public Lander(int initXSpeed, int initYSpeed, int initX, int initY, int initRotate, int initFuel, int initPower) {
            this.initXSpeed = initXSpeed;
            this.initYSpeed = initYSpeed;
            this.initX = initX;
            this.initY = initY;
            this.initRotate = initRotate;
            this.initFuel = initFuel;
            this.initPower = initPower;
        }

        public Lander(Lander l) {
            this.initXSpeed = l.initXSpeed;
            this.initYSpeed = l.initYSpeed;
            this.initX = l.initX;
            this.initY = l.initY;
            this.initRotate = l.initRotate;
            this.initFuel = l.initFuel;
            this.initPower = l.initPower;
        }

        int initFuel;
        int initPower;

        double xSpeed;
        double ySpeed;
        double x;
        double y;
        int rotate;
        int fuel;
        int power;
        int step = 0;
        int score = 0;

        List<Command> commands = new ArrayList<>(MAX_TIMESTEP);
        List<Point> points = new ArrayList<>(MAX_TIMESTEP);
        List<Point> speeds = new ArrayList<>(MAX_TIMESTEP);

        boolean isFlying = true;

        public void init() {
            this.rotate = initRotate;
            this.power = initPower;
            this.x = initX;
            this.y = initY;
            this.fuel = initFuel;
            this.xSpeed = initXSpeed;
            this.ySpeed = initYSpeed;
            points.clear();
            //   points.add(new Point(x,y));
            speeds.clear();
            // speeds.add(new Point((int)Math.ceil(xSpeed), (int)Math.ceil(ySpeed)));
            step = 0;
            score = 0;
            isFlying = true;
            lastDiff = 0;
        }

        public void tick(Level level) {
            long start = System.currentTimeMillis();
            if(!isFlying) {
                return;
            }
            step++;
            if (this.fuel < this.power) {
                this.power = this.fuel;
            }
            points.add(new Point(x, y));
            // speeds.add(new Point((int)Math.ceil(xSpeed), (int)Math.ceil(ySpeed)));

            this.fuel -= this.power;
            double arcAngle = -this.rotate * Math.PI / 180;
            double xacc = Math.sin(arcAngle) * this.power;
            double yacc = Math.cos(arcAngle) * this.power - level.g;
            //System.err.println(xacc +" " +yacc +" " + this.power + " " +this.rotate);
            this.xSpeed += xacc;
            this.ySpeed += yacc;
            this.x += this.xSpeed - (xacc * 0.5);
            this.y += this.ySpeed - (yacc * 0.5);
            if(y < 0 || y> 3000 || x<0 || x> 7000) {
                isFlying = false;
                // System.err.println("wyleciał za obszar");
                calculateScore(level, false);
                return;
            }

            for(int i =1; i < level.points.size();i++) {
                Point p3 = level.points.get(i-1);
                Point p4 = level.points.get(i);
                if (lineIntersection(points.get(step -1), new Point(x, y), p3, p4)) {
                    isFlying = false;
                    // points.set(step, new Point(x, y));
                    calculateScore(level, p3.y == p4.y);
                    // System.err.println("wyladowal " +name +";score "+ score);
                    break;
                }
            }
            //  System.err.println(System.currentTimeMillis() -start);
        }

        public void calculateScore(Level level, boolean landedOnFlat) {
            int score = 0;
            double currentSpeed = Math.sqrt(Math.pow(this.xSpeed, 2) + Math.pow(this.ySpeed, 2));

            if(!landedOnFlat) {
                Point p = points.get(step-1);
                Point to = level.middle;
            /*    if(p.x < level.minFlat.x) {
                    to = level.minFlat;
                }else {
                    to = level.maxFlat;
                }*/
                //score = 100 - (int)(100 * (p.distanceTo(to)/level.maxDistance));
                score = 100 - (int)(100 * (level.getDistanceToLandingArea(points.get(step -2))/level.maxDistance));
                int speedPen = (int)(0.1 * Math.max(currentSpeed - 100, 0));
                score-=speedPen;
            }else if(this.ySpeed < -40 || 20 < Math.abs(this.xSpeed)) {
                int xPen = 0;
                if (20 < Math.abs(this.xSpeed)) {
                    xPen = (int)(Math.abs(this.xSpeed) - 20) / 2;
                }
                int yPen = 0;
                if (this.ySpeed < -40) {
                    yPen = (int)(-40 - this.ySpeed) / 2;
                }
                this.score = 200 - xPen - yPen;
                return;
            }else if(Math.abs(this.rotate) >15){
                score = 300 - Math.abs(this.rotate)+15;
            }else {
                //  System.err.println("ySpeed " +ySpeed +";xSpeed "+ xSpeed);
                score = 400 - (int)(100 * fuel/initFuel);

            }
            this.score = score;
        }

        public void applyCommand(int next) {
            Command c = commands.get(next);
            int rotation = Math.round(c.rotate);
            rotation = Math.max(rotation, -90);
            rotation = Math.min(rotation, 90);

            if(this.rotate < 0 && rotation < 0) {
                if(this.rotate < rotation) {
                    rotation = Math.min(this.rotate + 15, rotation);
                }else {
                    rotation = Math.max(this.rotate - 15, rotation);
                }
            }else if(this.rotate < 0 && rotation > 0) {
                rotation = Math.min(this.rotate + 15, rotation);
            }else if(this.rotate >= 0 && rotation < 0) {
                rotation = Math.max(this.rotate -15, rotation);
            }else if(this.rotate >= 0 && rotation > 0) {
                if(this.rotate < rotation) {
                    rotation = Math.min(this.rotate + 15, rotation);
                }else{
                    rotation = Math.max(this.rotate - 15, rotation);
                }
            }
            else {
                this.rotate = rotation;
            }
            this.rotate = rotation;
            int power = (int)Math.round(c.power);
            if(power > this.power) {
                power = Math.min(this.power + 1, 4);
            }else if(power < this.power) {
                power = Math.max(0, this.power -1);
            }

            this.power = power;
          /*  double newPower = Math.round(c.power);
            newPower += this.lastDiff;
            int roundedPower = (int)Math.round(newPower);
            roundedPower = Math.max(Math.max(roundedPower, 0), this.power - 1);
            roundedPower = Math.min(Math.min(roundedPower, 4), this.power + 1);
            this.lastDiff = newPower - roundedPower;
            this.power = roundedPower;

            */

        }

        public void generateCommands(int count, int number) {
            //  commands = new ArrayList<>(count);
            int angle = this.rotate;
            int power = this.power;
            for (int i = 0; i < count; i++) {
                if(number <number/2) {
                    //if(number%2 == 0)
                    // angle += getRandomNumber(-5, 0);
                    angle +=10;
                    //    else
                    // angle += getRandomNumber(0, 16);
                    //   angle +=5;
                    // }else {
                    //    angle += getRandomNumber(-15, 16);
                    // }
                    // angle-=15;
                    angle = Math.min(angle,  30);
                    angle = Math.max(angle, -30);
                    power +=1;
                     power = Math.min(power,  4);
                //power = Math.max(power, 0);
                }else {
                    angle += getRandomNumber(-15, 16);
                    angle = Math.min(angle,  45);
                    angle = Math.max(angle, -45);
                    power += getRandomNumber(-1, 2);
                }
                //  double power = 5 * Math.random();
               // double power = getRandomNumber(0, 5);
               
                //power = Math.min(power,  4);
                //power = Math.max(power, 0);
                //System.err.println(power);
                this.commands.add(new Command(angle, power));
            }
        }

        public int getScore() {
            return score;
        }


        public double getCumulativeScore() {
            return cumulativeScore;
        }

        public void copyCommands(Lander lander) {
            for(int i = 0 ;i< lander.commands.size();i++) {
                this.commands.get(i).rotate = lander.commands.get(i).rotate;
                this.commands.get(i).power = lander.commands.get(i).power;
            }
        }
        public String toString() {
            return ""+commands;
        }
    }
    static Random random = new Random();
    public static int getRandomNumber(int min, int max) {
        return (int) ((random.nextInt(max - min)) + min);
    }
    //https://en.wikipedia.org/wiki/Line%E2%80%93line_intersection
    public static boolean lineIntersection(Point p1, Point p2, Point p3, Point p4) {
        double denominator = (p1.x - p2.x) * (p3.y - p4.y) - (p1.y - p2.y) * (p3.x - p4.x);
        double t = ((p1.x - p3.x) * (p3.y - p4.y) - (p1.y - p3.y) * (p3.x - p4.x))/denominator;
        double u = ((p2.x - p1.x) * (p1.y - p3.y) - (p2.y - p1.y) * (p1.x - p3.x))/denominator;
        if(t>= 0 && t<=1 && u>=0 && u<=1) {
            return true;
        }
        return false;
    }

    static int binarySearch(List<Lander> landers, int l, int r, double x)
    {
        //System.err.println(l +" " +r + " " +x);
        if (r >= l) {
            int mid = l + (r - l) / 2;

            // If the element is present at the
            // middle itself
            if (equalsDouble(landers.get(mid).cumulativeScore , x))
                return mid;

            // If element is smaller than mid, then
            // it can only be present in left subarray
            if (landers.get(mid).cumulativeScore < x)
                return binarySearch(landers, l, mid - 1, x);

            // Else the element can only be present
            // in right subarray
            return binarySearch(landers, mid + 1, r, x);
        }

        // We reach here when element is not present
        // in array
        return -1;
    }

    static void smooth(Lander child){
        if (Math.random() < 0.2) {
            // Angle
            int x = getRandomNumber(0, child.commands.size() - 3);
            int avg = (child.commands.get(x).rotate + child.commands.get(x+1).rotate +child.commands.get(x+2).rotate) / 3;
            child.commands.get(x).rotate = avg;
            child.commands.get(x+1).rotate = avg;
            child.commands.get(x+2).rotate = avg;
        }
        if (Math.random() < 0.2) {
            // Power
            int x = getRandomNumber(0, child.commands.size() - 3);
            double avg = (child.commands.get(x).power + child.commands.get(x+1).power +child.commands.get(x+2).power) / 3;
            child.commands.get(x).power = avg;
            child.commands.get(x+1).power = avg;
            child.commands.get(x+2).power = avg;
        }
    }

    static double epsilon = 0.01;
    static boolean equalsDouble(double a, double b) {
        //System.err.println(a +" " +b + " " +Math.abs(a-b) +" " +epsilon);
        if (Math.abs(a-b) <= epsilon) return true;
        return false;
    }

    public static void createNewPopulation(int pop, Population  currentPop, Population nextPop) {
          /* for (int p = 0; p < NUMBER_OF_POPULATIONS; p++) {
                for (int i = REPRODUCING_LANDERS; i < LANDERS_IN_POPULATION; i++) {
                    // Replace each low-value lander with a combination of two high-value landers
                    int combinationCount = REPRODUCING_LANDERS * (REPRODUCING_LANDERS - 1);
                    int combination = (i - REPRODUCING_LANDERS) % combinationCount;
                    int momIndex = (int)Math.floor(combination / (REPRODUCING_LANDERS - 1));  // The "row"
                    int dadIndex = combination % (REPRODUCING_LANDERS - 1);  // The "col"
                    if (momIndex <= dadIndex) {
                        // Indexes have to be different
                        dadIndex += 1;
                    }
                    int populationOffset = p * LANDERS_IN_POPULATION;
                    landers.get(populationOffset + i).commands = crossAndMutate(landers.get(populationOffset + momIndex), landers.get(populationOffset + dadIndex)).commands;
                }
            }

            // Maybe mix two populations
           /* int combinationCount = NUMBER_OF_POPULATIONS * (NUMBER_OF_POPULATIONS - 1);
            for (int combination = 0; combination < combinationCount; combination++) {
                if (Math.random() < MIX_POPULATION_CHANCE) {
                    int p1 = (int)Math.floor(combination / (NUMBER_OF_POPULATIONS - 1));
                    int p2 = combination % (NUMBER_OF_POPULATIONS - 1);

                    // Have a child from the two best landers and place it randomly
                     System.err.println(p1 * LANDERS_IN_POPULATION + REPRODUCING_LANDERS +" " +((p1 + 1) * LANDERS_IN_POPULATION - 1));
                    int childIndex = getRandomNumber(p1 * LANDERS_IN_POPULATION + REPRODUCING_LANDERS, (p1 + 1) * LANDERS_IN_POPULATION - 1);
                    landers.get(childIndex).commands = crossAndMutate(landers.get(p1 * LANDERS_IN_POPULATION), landers.get(p2 * LANDERS_IN_POPULATION)).commands;

                }
            }
            return landers;*/
        //  currentPop.calculateScores();
        List<Lander> currentLanders = currentPop.landers;
        List<Lander> nextLanders = nextPop.landers;
        //currentLanders.sort(Comparator.comparingInt(Lander::getScore).reversed());
        System.err.println("BEST SCORE " +currentLanders.get(0).score);
        int eliteIndex = (int)(currentLanders.size() * 0.3);
        for(int i = 0 ;i<eliteIndex;i++) {
            nextLanders.get(i).copyCommands(currentLanders.get(i));
        }/*
        for(int i = eliteIndex ;i<(currentLanders.size()/2);i++) {
            int id1 = getRandomNumber(eliteIndex, currentLanders.size());
             //System.err.println(id1);
            nextLanders.get(i).copyCommands(currentLanders.get(id1));
        }*/
        int childIndex = eliteIndex;
        //Set<Integer> indeses = new TreeSet<>();
        // Random r = new Random();
        for(int j = 0;j<currentLanders.size()/2;j +=2) {
            // while(newPopulation.size() < landers.size()) {
            //cross two random chromosomes
       /*     int index = binarySearch(currentLanders, 0, currentLanders.size() - 1, r.nextDouble());
            while(index == -1) {
                index = binarySearch(currentLanders, 0, currentLanders.size() -1, r.nextDouble());
            }
            int index2 = binarySearch(currentLanders, 0, currentLanders.size() - 1, r.nextDouble());
            while(index == index2 || index2 == -1) {
                index2 = binarySearch(currentLanders, 0, currentLanders.size() -1, r.nextDouble());
            }*/
            Lander mom;
            Lander dad;
            if(Math.random() <0.5) {
                mom  = currentLanders.get(j);
                dad = currentLanders.get(j+1);
            }else{
                mom  = currentLanders.get(j+1);
                dad = currentLanders.get(j);
            }
            int minTimestep = Math.min(mom.step, dad.step);
            int crossoverFrom = (int)Math.floor(minTimestep / 5);
            int crossoverTo = (int)Math.floor(minTimestep * 4 / 5);
            //int crossoverIndex = getRandomNumber(crossoverFrom, crossoverTo);
            int crossoverIndex = getRandomNumber(3, mom.commands.size() -3);

            // System.err.println(crossoverIndex);
            double m =0.8;
            for(int i =0 ;i<crossoverIndex;i++) {
                nextLanders.get(childIndex).commands.get(i).copyCommand(mom.commands.get(i));
                nextLanders.get(childIndex+1).commands.get(i).copyCommand(dad.commands.get(i));
                if (Math.random() < m) {
                    nextLanders.get(childIndex).commands.get(i).power  += getRandomNumber(0, 5);
                    nextLanders.get(childIndex).commands.get(i).rotate  += getRandomNumber(-10, 11);
                }
                if (Math.random() < m) {
                    nextLanders.get(childIndex+1).commands.get(i).power  += getRandomNumber(0, 5);
                    nextLanders.get(childIndex+1).commands.get(i).rotate  += getRandomNumber(-10, 11);
                }

            }
            for(int i =crossoverIndex ;i< mom.commands.size();i++) {
                nextLanders.get(childIndex).commands.get(i).copyCommand(dad.commands.get(i));
                nextLanders.get(childIndex+1).commands.get(i).copyCommand(mom.commands.get(i));
                if (Math.random() < m) {
                    nextLanders.get(childIndex).commands.get(i).power  += getRandomNumber(0, 5);
                    nextLanders.get(childIndex).commands.get(i).rotate  += getRandomNumber(-10, 11);
                }
                if (Math.random() < m) {
                    nextLanders.get(childIndex+1).commands.get(i).power  += getRandomNumber(0, 5);
                    nextLanders.get(childIndex+1).commands.get(i).rotate  += getRandomNumber(-10, 11);
                }

            }
/*
                    for(int i =0 ;i< mom.commands.size();i++) {
            Command momC = mom.commands.get(i);
            Command dadC = dad.commands.get(i);
            double random = Math.random();
            int rotate1 = (int)(random * momC.rotate + (1 - random) * dadC.rotate);
            int rotate2 = (int)((1 - random) * momC.rotate + (random) * dadC.rotate);
            double power1 = (random * momC.power + (1 - random) * dadC.power);
            double power2 = (((1 - random) * momC.power) + ((random) * dadC.power));
            nextLanders.get(childIndex).commands.get(i).power = power1;
            nextLanders.get(childIndex).commands.get(i).rotate = rotate1;
            nextLanders.get(childIndex+1).commands.get(i).power = power2;
            nextLanders.get(childIndex+1).commands.get(i).rotate = rotate2;
        }*/

            //smooth(nextLanders.get(childIndex));
            //smooth(nextLanders.get(childIndex+1));
            //mutate( nextLanders.get(childIndex),mom, dad);
            //  mutate( nextLanders.get(childIndex +1),mom, dad);
            childIndex+=2;
           /* for(Lander child: children) {
                if (Math.random() < 0.2) {
                    // Angle
                    int x = getRandomNumber(0, child.commands.size() - 3);
                    int avg = (child.commands.get(x).rotate + child.commands.get(x+1).rotate +child.commands.get(x+2).rotate) / 3;
                    child.commands.get(x).rotate = avg;
                    child.commands.get(x+1).rotate = avg;
                    child.commands.get(x+2).rotate = avg;
                }
                if (Math.random() < 0.2) {
                    // Power
                    int x = getRandomNumber(0, child.commands.size() - 3);
                    double avg = (child.commands.get(x).power + child.commands.get(x+1).power +child.commands.get(x+2).power) / 3;
                    child.commands.get(x).power = avg;
                    child.commands.get(x+1).power = avg;
                    child.commands.get(x+2).power = avg;
                }

            }*/

        }
        // System.err.println(childIndex);
        if(pop == NUMBER_OF_POPULATIONS -1) {
            // System.err.println("N POPULACJA " + nextPop.landers.get((currentLanders.size()/2)+2));
            // System.err.println("P POPULACJA " + currentPop.landers.get((currentLanders.size()/2)+2));
        }
    }

    private static void mutate(Lander child, Lander mom, Lander dad) {
        //mutattion
        for (int j = 0; j < child.commands.size(); j++) {
            Command c = child.commands.get(j);
             /*   int scoreMultiplier = 5;
                if (100 < Math.max(mom.score, dad.score)) {
                    scoreMultiplier = 3;
                }
                if (200 < Math.max(mom.score, dad.score)) {
                    scoreMultiplier = 1;
                }
                double progress = j / child.commands.size();
                double progressChance = 0.4 + 1.0 * progress + 10 * Math.pow(progress, 2);
                double mutationChance = 0.05 * scoreMultiplier * progressChance;
               */ // System.err.println(mom.score +" " + dad.score +" " + mutationChance);
            if (Math.random() < 0.9) {
                // System.err.println("NOWA Mutacja ");
                //c.power += Math.random() - 0.5;
                c.power += getRandomNumber(-1, 2);
                //c.power = Math.min(c.power,  4);
                //c.power = Math.max(c.power, 0);
                int angle = c.rotate;
                angle += getRandomNumber(00, 16);
                c.rotate = angle;
                // break;
            }
        }
    }

    private static Lander crossAndMutate(Player.Lander mom, Player.Lander dad) {
        Lander child = new Lander(mom);

        int minTimestep = Math.min(mom.step, mom.step);
        int crossoverFrom = (int)Math.floor(minTimestep / 5);
        int crossoverTo = (int)Math.floor(minTimestep * 4 / 5);
        int crossoverIndex = getRandomNumber(crossoverFrom, crossoverTo +1);
        // System.err.println(crossoverIndex);
        //int split = getRandomNumber(0, mom.commands.size());
        for(int i =0 ;i<crossoverIndex;i++) {
            child.commands.add(new Command(mom.commands.get(i)));
        }
        for(int i =crossoverIndex ;i< mom.commands.size();i++) {
            child.commands.add(new Command(dad.commands.get(i)));
        }
        if (Math.random() < 0.2) {
            // Angle
            int x = getRandomNumber(0, child.commands.size() - 3);
            int avg = (child.commands.get(x).rotate + child.commands.get(x+1).rotate +child.commands.get(x+2).rotate) / 3;
            child.commands.get(x).rotate = avg;
            child.commands.get(x+1).rotate = avg;
            child.commands.get(x+2).rotate = avg;
        }
        if (Math.random() < 0.2) {
            // Power
            int x = getRandomNumber(0, child.commands.size() - 3);
            double avg = (child.commands.get(x).power + child.commands.get(x+1).power +child.commands.get(x+2).power) / 3;
            child.commands.get(x).power = avg;
            child.commands.get(x+1).power = avg;
            child.commands.get(x+2).power = avg;
        }
        //mutattion
        for(int j = 0; j<child.commands.size();j++) {
            Command c = child.commands.get(j);
            int scoreMultiplier = 5;
            if (100 < Math.max(mom.score, dad.score)) {
                scoreMultiplier = 3;
            }
            if (200 < Math.max(mom.score, dad.score)) {
                scoreMultiplier = 1;
            }
            double progress = j / child.commands.size();
            double progressChance = 0.4 + 1.0 * progress + 10 * Math.pow(progress, 2);
            double mutationChance = 0.05 * scoreMultiplier * progressChance;
            // System.err.println(mom.score +" " + dad.score +" " + mutationChance);
            if(Math.random() < mutationChance) {
                c.power += getRandomNumber(-1, 2);
                int angle = c.rotate;
                angle += getRandomNumber(-15, 16);
                c.rotate = angle;
            }
        }
        return child;
    }
    public static List<Lander> cross(Lander mom, Lander dad, int pop, int j) {
        Lander child1 = new Lander(mom);
        Lander child2 = new Lander(mom);
        // child.name = "LANDER " + j+"->POP->" +pop;
        //           int minTimestep = Math.min(mom.step, mom.step);
        //    int crossoverFrom = (int)Math.floor(minTimestep / 5);
        //    int crossoverTo = (int)Math.floor(minTimestep * 4 / 5);
        //    int crossoverIndex = getRandomNumber(crossoverFrom, crossoverTo);
        // System.err.println(crossoverIndex);
        //int split = getRandomNumber(0, mom.commands.size());
        // for(int i =0 ;i<crossoverIndex;i++) {
        // child.commands.add(new Command(mom.commands.get(i)));
        // }
        // for(int i =crossoverIndex ;i< mom.commands.size();i++) {
        //  child.commands.add(new Command(dad.commands.get(i)));
        //}
        for(int i =0 ;i< mom.commands.size();i++) {
            Command momC = mom.commands.get(i);
            Command dadC = dad.commands.get(i);
            double random = Math.random();
            int rotate1 = (int)(random * momC.rotate + (1 - random) * dadC.rotate);
            int rotate2 = (int)((1 - random) * momC.rotate + (random) * dadC.rotate);
            double power1 = (random * momC.power + (1 - random) * dadC.power);
            double power2 = (((1 - random) * momC.power) + ((random) * dadC.power));
            child1.commands.add(new Command(rotate1, power1));
            child2.commands.add(new Command(rotate2, power2));
        }
        //System.err.println(child.commands.size());
        List<Lander> l = new ArrayList<>();
        l.add(child1);
        l.add(child2);
        return l;
    }


    public static void main(String args[]) {
        Scanner in = new Scanner(System.in);
        int surfaceN = in.nextInt(); // the number of points used to draw the surface of Mars.
        int H = 3000;
        int startFlat = 0;
        int endFlat = 7000;

        int landers = NUMBER_OF_LANDERS;
        int commands = MAX_TIMESTEP;
        int populations = NUMBER_OF_POPULATIONS;

        boolean isFirst = true;
        boolean found = false;
        Level level = new Level();
        int lastScore = 0;
        int count =0;
        int firstI = 0;
        for (int i = 0; i < surfaceN; i++) {
            int landX = in.nextInt(); // X coordinate of a surface point. (0 to 6999)
            int landY = in.nextInt(); // Y coordinate of a surface point. By linking all the points together in a sequential fashion, you form the surface of Mars.
            //System.err.format("%d %d %d %d\n", landX, landY, startFlat, endFlat);
            if(landY == H) {
                if(!isFirst) {
                    endFlat = landX;
                    found = true;
                }
            }else if(!found) {
                H=landY;
                startFlat = landX;
                isFirst = false;
                firstI = i;
            }
            level.points.add(new Point(landX, landY));
            level.minFlat = new Point(startFlat, H);
            level.maxFlat = new Point(endFlat, H);
            level.li = firstI;
            level.init();
        }
        // System.err.format("%d %d", startFlat, endFlat);
        Lander l = null;
        boolean init = true;
        int tick = 0;
        // game loop
        while (true) {
            int X = in.nextInt();
            int Y = in.nextInt();
            int hSpeed = in.nextInt(); // the horizontal speed (in m/s), can be negative.
            int vSpeed = in.nextInt(); // the vertical speed (in m/s), can be negative.
            int fuel = in.nextInt(); // the quantity of remaining fuel in liters.
            int rotate = in.nextInt(); // the rotation angle in degrees (-90 to 90).
            int power = in.nextInt(); // the thrust power (0 to 4).
            if(init) {
                init = false;
                Population p1 = new Population();
                p1.name = "1";
                Population p2 = new Population();
                p2.name = "2";
                List<Lander> population1 = new ArrayList<>(NUMBER_OF_LANDERS);
                p1.landers = population1;
                List<Lander> population2 = new ArrayList<>(NUMBER_OF_LANDERS);
                p2.landers = population2;
                List<Lander> currentPopulation = null;
                for(int k =0;k<populations;k++) {
                    if(k == 0) {
                        long start = System.currentTimeMillis();
                        for (int j = 0; j < landers; j++) {
                            l = new Lander(hSpeed, vSpeed, X, Y, rotate, fuel, power);
                            l.init();
                            l.generateCommands(commands, j);
                            l.name = "LANDER " + j+"->POP->INIT";
                            population1.add(l);
                            l = new Lander(hSpeed, vSpeed, X, Y, rotate, fuel, power);
                            l.init();
                            l.generateCommands(commands, j);
                            l.name = "LANDER " + j+"->POP->INIT";
                            population2.add(l);
                        }
                        //System.err.println(System.currentTimeMillis() -start);
                    }else {
                        if(k%2 == 0) {
                            createNewPopulation(k, p2, p1);
                        }else {
                            createNewPopulation(k, p1, p2);
                        }
                    }
                    if(k%2 == 0) {
                        // System.err.println("populacja 1");
                        currentPopulation = population1;
                    }else {
                        currentPopulation = population2;
                    }
//long start = System.currentTimeMillis();
                    for (int j = 0; j < landers; j++) {

                        l = currentPopulation.get(j);
                        l.init();
                        for (int i = 0; i < commands; i++) {
                            l.applyCommand(i);
                            l.tick(level);
                        }
                        //System.err.println("nie dolecial" + l.isFlying + " " + l.score);
                        if(l.score == 0 && l.isFlying) {
                            // System.err.println("nie dolecial");
                            //   l.calculateScore(level, false);
                        }

                        //System.err.println(l.name +" where " + l.x +" " +l.y);
                    }
                    // System.err.println(System.currentTimeMillis() -start);
                  /*  List<Population> pops = new ArrayList<>();
                    for (int p = 0; p < NUMBER_OF_POPULATIONS; p++) {
                        Population pop = new Population();
                        pop.landers.addAll(population.subList(p*LANDERS_IN_POPULATION, (p+1)*LANDERS_IN_POPULATION));
                        pop.landers.sort(Comparator.comparingInt(Lander::getScore).reversed());
                        pops.add(pop);
                    }
                    pops.sort(Comparator.comparing(Population::getBestScore).reversed());
                    population.clear();
                    for(Population pop: pops) {
                        population.addAll(pop.landers);
                    }*/
                    //  System.err.println("POP ISZE" + " "+population.size());
                    currentPopulation.sort(Comparator.comparingInt(Lander::getScore).reversed());
                    if(currentPopulation.get(0).score > 300 && lastScore == currentPopulation.get(0).score) {
                        count++;
                    }else {
                        lastScore = currentPopulation.get(0).score;
                        count = 0;
                    }
                    if(count == 3) {
                        break;
                    }
                }
                currentPopulation.sort(Comparator.comparingInt(Lander::getScore).reversed());
                l = currentPopulation.get(0);
                System.err.println("SCORE" + " "+l.score + " " + l.step);
                l.power = l.initPower;
                l.rotate = l.initRotate;
                ///  l.init();
            }
            System.err.format("%d %d %d %d %d %d\n", hSpeed, vSpeed, X, Y,  power, rotate);
            int a = (Math.abs(l.rotate))/15 +1;
            System.err.println(a +" " +tick + " " + (l.step - tick));
            if(tick +1 +a < l.step) {
                //  System.err.println(l.speeds.get(tick) +" " +l.points.get(tick) + " "+ l.power +" " +l.rotate);
                // print(l.rotate, l.power);
                l.applyCommand(tick++);
                System.out.println(l.rotate +" " +l.power);
            }else {
                System.out.println("0 4");
            }
            // Write an action using System.out.println()
            // To debug: System.err.println("Debug messages...");
            // if(vSpeed != 0) {

            //  }// rotate power. rotate is the desired rotation angle. power is the desired thrust power.
/*
            if(((X +vSpeed *4) > startFlat) && ((X +vSpeed *70) < endFlat)) {
                if(hSpeed < -70) {
                    System.out.println("-70 4");
                }else
                if(hSpeed < -60) {
                    System.out.println("-60 4");
                }else
                if(hSpeed < -50) {
                    System.out.println("-40 4");
                }else
                if(hSpeed < -30) {
                    System.out.println("-30 4");
                }
                else {
                    if(vSpeed < -30) {
                        System.out.println("-10 4");
                    }else {
                        System.out.println("0 3");
                    }
                }
            }else{
                System.out.println("-30 3");
            }*/
        }
    }

    static double lastXSpeed = 0;
    static double lastYSpeed = 0;

    public static void print(int rotate, double power){
        double arcAngle = -rotate * Math.PI / 180;
        double xacc = Math.sin(arcAngle) * power;
        double yacc = Math.cos(arcAngle) * (power - 3.711);
        double xSpeed = lastXSpeed + xacc;
        double ySpeed = lastYSpeed + yacc;
        System.err.println(xacc +" " +yacc +" " + lastXSpeed + " " +lastYSpeed + " " + xSpeed + " " +ySpeed);
        lastXSpeed = xSpeed;
        lastYSpeed = ySpeed;
        //  this.x += Math.ceil(this.xSpeed - (xacc * 0.5));
        // this.y += Math.ceil(this.ySpeed - (yacc * 0.5));
    }
}
