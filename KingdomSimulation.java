// Kingdom Simulation - Main class
import java.util.Random;

public class KingdomSimulation {
    public static void main(String[] args) {
        // Create two kingdoms
        Kingdom kingdom1 = new Kingdom("Kingdom 1");
        Kingdom kingdom2 = new Kingdom("Kingdom 2");

        // Start simulation
        kingdom1.startSimulation();
        kingdom2.startSimulation();

        // Start war between kingdoms
        War war = new War(kingdom1, kingdom2);
        war.startWar();

        try {
            // Wait for the war to end naturally (one kingdom defeats the other)
            war.waitForEnd();

            // After the war ends, give a short time to see final state
            Thread.sleep(3000);

            // End simulation
            kingdom1.endSimulation();
            kingdom2.endSimulation();
            war.endWar();

            System.out.println("Simulation ended.");
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}

// Kingdom class - represents a kingdom with all its entities
class Kingdom {
    private String name;
    private boolean running = true;
    private Random random = new Random(); // Add random generator

    // Resources
    private Resource coal = new Resource("Coal", 0);
    private Resource ore = new Resource("Ore", 0);
    private Resource metal = new Resource("Metal", 0);
    private Resource weapons = new Resource("Weapons", 0);
    private Resource jewelry = new Resource("Jewelry", 0);
    private Resource food = new Resource("Food", 0);
    private Resource happiness = new Resource("Happiness", 0);
    private Resource tactics = new Resource("Tactics", 0);

    // Combat strength represents the kingdom's military power
    private int combatStrength = 0;

    // Entities
    private Mine mine;
    private Blacksmith blacksmith;
    private Farm farm;
    private Princess princess;
    private King king;
    private Army army;
    private Jeweler jeweler;

    // List of all entity threads
    private Thread[] entityThreads;

    public Kingdom(String name) {
        this.name = name;

        // Create entities
        mine = new Mine(this, coal, ore);
        blacksmith = new Blacksmith(this, ore, metal, weapons);
        farm = new Farm(this, food);
        jeweler = new Jeweler(this, ore, jewelry);
        princess = new Princess(this, jewelry, happiness);
        king = new King(this, happiness, tactics);
        army = new Army(this, weapons, food, tactics);

        // Create threads for all entities
        entityThreads = new Thread[] {
                new Thread(mine, name + " - Mine"),
                new Thread(blacksmith, name + " - Blacksmith"),
                new Thread(farm, name + " - Farm"),
                new Thread(jeweler, name + " - Jeweler"),
                new Thread(princess, name + " - Princess"),
                new Thread(king, name + " - King"),
                new Thread(army, name + " - Army")
        };
    }

    public String getName() {
        return name;
    }

    public void startSimulation() {
        System.out.println(name + " simulation started.");
        for (Thread t : entityThreads) {
            t.start();
        }
    }

    public void endSimulation() {
        running = false;
        for (Thread t : entityThreads) {
            try {
                t.join(1000); // Wait up to 1 second for each thread to finish
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public boolean isRunning() {
        return running;
    }

    public int getCombatStrength() {
        return combatStrength;
    }

    public void increaseCombatStrength(int amount) {
        synchronized (this) {
            combatStrength += amount;
        }
    }

    public Army getArmy() {
        return army;
    }

    // Added method to get a random number
    public int getRandomNumber(int min, int max) {
        return random.nextInt(max - min + 1) + min;
    }
}

// Resource class - represents a resource with proper synchronization
class Resource {
    private String name;
    private int amount;

    public Resource(String name, int initialAmount) {
        this.name = name;
        this.amount = initialAmount;
    }

    public String getName() {
        return name;
    }

    // Synchronized method to produce resources
    public synchronized void produce(int count, String producer) {
        amount += count;
        System.out.println(producer + " produced " + count + " " + name + ". Total: " + amount);
        // Notify all waiting consumers
        notifyAll();
    }

    // Synchronized method to consume resources
    public synchronized boolean consume(int count, String consumer) {
        // Wait until enough resources are available
        while (amount < count) {
            try {
                System.out.println(consumer + " is waiting for " + count + " " + name + ". Available: " + amount);
                wait();
            } catch (InterruptedException e) {
                return false;
            }
        }

        // Consume resources
        amount -= count;
        System.out.println(consumer + " consumed " + count + " " + name + ". Remaining: " + amount);
        return true;
    }

    public synchronized int getAmount() {
        return amount;
    }
}

// Base class for all entities in the kingdom
abstract class Entity implements Runnable {
    protected Kingdom kingdom;
    protected String entityName;
    protected Random random = new Random(); // Add random generator

    public Entity(Kingdom kingdom, String entityName) {
        this.kingdom = kingdom;
        this.entityName = kingdom.getName() + " " + entityName;
    }

    @Override
    public void run() {
        try {
            while (kingdom.isRunning()) {
                // Each entity has its own production cycle
                performAction();

                // Sleep to simulate production time with random variation
                long baseTime = getProductionTime();
                long randomVariation = (long)(baseTime * 0.5 * random.nextDouble()); // Up to 50% random variation
                boolean faster = random.nextBoolean();
                long actualTime = faster ?
                        baseTime - randomVariation : // Faster production
                        baseTime + randomVariation;  // Slower production

                Thread.sleep(Math.max(500, actualTime)); // Minimum 0.5 seconds
            }
        } catch (InterruptedException e) {
            // Thread was interrupted, exit gracefully
        }
    }

    // Main action method to be implemented by each entity
    protected abstract void performAction() throws InterruptedException;

    // Production time in milliseconds
    protected abstract long getProductionTime();
}

// Mine produces coal and ore
class Mine extends Entity {
    private Resource coal;
    private Resource ore;

    public Mine(Kingdom kingdom, Resource coal, Resource ore) {
        super(kingdom, "Mine");
        this.coal = coal;
        this.ore = ore;
    }

    @Override
    protected void performAction() {
        // Produce coal and ore with random variations
        int coalAmount = kingdom.getRandomNumber(1, 5); // 1-5 coal
        int oreAmount = kingdom.getRandomNumber(1, 3);  // 1-3 ore

        coal.produce(coalAmount, entityName);
        ore.produce(oreAmount, entityName);
    }

    @Override
    protected long getProductionTime() {
        return 2000; // 2 seconds base time
    }
}

// Blacksmith consumes ore and produces metal and weapons
class Blacksmith extends Entity {
    private Resource ore;
    private Resource metal;
    private Resource weapons;

    public Blacksmith(Kingdom kingdom, Resource ore, Resource metal, Resource weapons) {
        super(kingdom, "Blacksmith");
        this.ore = ore;
        this.metal = metal;
        this.weapons = weapons;
    }

    @Override
    protected void performAction() throws InterruptedException {
        // Random ore consumption (1-3)
        int oreNeeded = kingdom.getRandomNumber(1, 3);

        // Consume ore
        if (ore.consume(oreNeeded, entityName)) {
            // Produce metal (with better efficiency for larger batches)
            int metalProduced = Math.max(1, oreNeeded / 2 + kingdom.getRandomNumber(0, 1));
            metal.produce(metalProduced, entityName);

            // Randomly decide whether to make weapons
            if (random.nextDouble() < 0.8) { // 80% chance to make weapons
                // Use some metal to make weapons
                if (metal.consume(1, entityName)) {
                    int weaponsProduced = kingdom.getRandomNumber(1, 2);
                    weapons.produce(weaponsProduced, entityName);
                }
            }
        }
    }

    @Override
    protected long getProductionTime() {
        return 3000; // 3 seconds base time
    }
}

// Farm produces food
class Farm extends Entity {
    private Resource food;

    public Farm(Kingdom kingdom, Resource food) {
        super(kingdom, "Farm");
        this.food = food;
    }

    @Override
    protected void performAction() {
        // Simulate random harvest with seasonal variations
        int baseProduction = kingdom.getRandomNumber(3, 8);

        // Random chance for bumper crops or poor harvests
        double harvestLuck = random.nextDouble();
        int actualProduction;

        if (harvestLuck < 0.1) {
            // Poor harvest (10% chance)
            actualProduction = Math.max(1, baseProduction / 2);
            System.out.println(entityName + " experienced a poor harvest!");
        } else if (harvestLuck > 0.9) {
            // Bumper crop (10% chance)
            actualProduction = baseProduction * 2;
            System.out.println(entityName + " experienced a bumper crop!");
        } else {
            // Normal harvest (80% chance)
            actualProduction = baseProduction;
        }

        food.produce(actualProduction, entityName);
    }

    @Override
    protected long getProductionTime() {
        return 1500; // 1.5 seconds base time
    }
}

// Jeweler consumes ore and produces jewelry
class Jeweler extends Entity {
    private Resource ore;
    private Resource jewelry;

    public Jeweler(Kingdom kingdom, Resource ore, Resource jewelry) {
        super(kingdom, "Jeweler");
        this.ore = ore;
        this.jewelry = jewelry;
    }

    @Override
    protected void performAction() throws InterruptedException {
        // Random chance for higher quality work requiring more ore
        boolean highQualityWork = random.nextDouble() > 0.7; // 30% chance
        int oreNeeded = highQualityWork ? 2 : 1;

        // Consume ore to make jewelry
        if (ore.consume(oreNeeded, entityName)) {
            int jewelryProduced = highQualityWork ?
                    kingdom.getRandomNumber(2, 3) : // Better yield for high quality work
                    1; // Standard yield

            if (highQualityWork) {
                System.out.println(entityName + " created high-quality jewelry!");
            }

            jewelry.produce(jewelryProduced, entityName);
        }
    }

    @Override
    protected long getProductionTime() {
        return 4000; // 4 seconds base time
    }
}

// Princess consumes jewelry and produces happiness
class Princess extends Entity {
    private Resource jewelry;
    private Resource happiness;

    public Princess(Kingdom kingdom, Resource jewelry, Resource happiness) {
        super(kingdom, "Princess");
        this.jewelry = jewelry;
        this.happiness = happiness;
    }

    @Override
    protected void performAction() throws InterruptedException {
        // Consume jewelry
        if (jewelry.consume(1, entityName)) {
            // Produce happiness with mood variations
            int moodFactor = kingdom.getRandomNumber(1, 5);

            if (moodFactor == 5) {
                System.out.println(entityName + " is in an excellent mood today!");
                happiness.produce(5, entityName);
            } else if (moodFactor == 1) {
                System.out.println(entityName + " is in a poor mood today.");
                happiness.produce(1, entityName);
            } else {
                happiness.produce(moodFactor, entityName);
            }
        }
    }

    @Override
    protected long getProductionTime() {
        return 2500; // 2.5 seconds base time
    }
}

// King consumes happiness and produces tactics
class King extends Entity {
    private Resource happiness;
    private Resource tactics;

    public King(Kingdom kingdom, Resource happiness, Resource tactics) {
        super(kingdom, "King");
        this.happiness = happiness;
        this.tactics = tactics;
    }

    @Override
    protected void performAction() throws InterruptedException {
        // Random happiness requirement based on king's mood
        int happinessNeeded = kingdom.getRandomNumber(1, 3);

        // Consume happiness
        if (happiness.consume(happinessNeeded, entityName)) {
            // Produce tactics based on inspiration
            double inspiration = random.nextDouble();
            int tacticsProduced;

            if (inspiration > 0.9) {
                // Brilliant strategy (10% chance)
                tacticsProduced = 3;
                System.out.println(entityName + " had a brilliant strategic insight!");
            } else if (inspiration < 0.2) {
                // Basic strategy (20% chance)
                tacticsProduced = 1;
            } else {
                // Standard strategy (70% chance)
                tacticsProduced = 2;
            }

            tactics.produce(tacticsProduced, entityName);
        }
    }

    @Override
    protected long getProductionTime() {
        return 5000; // 5 seconds base time
    }
}

// Army consumes weapons, food, and tactics, and builds combat strength
class Army extends Entity {
    private Resource weapons;
    private Resource food;
    private Resource tactics;
    private int strength = 0;
    private boolean defeated = false;

    public Army(Kingdom kingdom, Resource weapons, Resource food, Resource tactics) {
        super(kingdom, "Army");
        this.weapons = weapons;
        this.food = food;
        this.tactics = tactics;
    }

    @Override
    protected void performAction() throws InterruptedException {
        // Randomize resource requirements
        int weaponsNeeded = kingdom.getRandomNumber(1, 2);
        int foodNeeded = kingdom.getRandomNumber(2, 4);
        int tacticsNeeded = 1; // Tactics always needed

        // Consume resources to build strength
        boolean hasWeapons = weapons.consume(weaponsNeeded, entityName);
        boolean hasFood = food.consume(foodNeeded, entityName);
        boolean hasTactics = tactics.consume(tacticsNeeded, entityName);

        // If all resources were consumed, increase strength
        if (hasWeapons && hasFood && hasTactics) {
            // Calculate strength increase based on resources consumed
            int baseIncrease = weaponsNeeded * 2 + tacticsNeeded * 3;

            // Random training effectiveness
            double trainingEffectiveness = 0.8 + random.nextDouble() * 0.4; // 80-120% effectiveness
            int actualIncrease = (int)(baseIncrease * trainingEffectiveness);

            if (trainingEffectiveness > 1.1) {
                System.out.println(entityName + " had an excellent training session!");
            }

            increaseStrength(actualIncrease);
            System.out.println(entityName + " increased strength by " + actualIncrease + " to " + strength);
            kingdom.increaseCombatStrength(actualIncrease);
        }
    }

    public synchronized void increaseStrength(int amount) {
        strength += amount;
    }

    public synchronized void reduceStrength(int amount) {
        strength = Math.max(0, strength - amount);
    }

    public synchronized int getStrength() {
        return strength;
    }

    public synchronized void setDefeated(boolean defeated) {
        this.defeated = defeated;
    }

    public synchronized boolean isDefeated() {
        return defeated;
    }

    @Override
    protected long getProductionTime() {
        return 4000; // 4 seconds base time
    }
}

// War class to manage the war between two kingdoms
class War implements Runnable {
    private Kingdom kingdom1;
    private Kingdom kingdom2;
    private boolean warActive = true;
    private Thread warThread;
    private boolean warEnded = false;
    private Random random = new Random(); // Add random generator

    public War(Kingdom kingdom1, Kingdom kingdom2) {
        this.kingdom1 = kingdom1;
        this.kingdom2 = kingdom2;
        this.warThread = new Thread(this, "War Thread");
    }

    public void startWar() {
        warActive = true;
        warThread.start();
        System.out.println("WAR STARTED between " + kingdom1.getName() + " and " + kingdom2.getName());
    }

    public void endWar() {
        warActive = false;
        try {
            warThread.join(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    // Method to wait until war ends naturally
    public synchronized void waitForEnd() throws InterruptedException {
        while (!warEnded) {
            wait();
        }
    }

    @Override
    public void run() {
        try {
            // Give kingdoms some time to build up before starting battles
            Thread.sleep(18000);

            while (warActive) {
                // Conduct battle
                battle();

                // Check if one kingdom has been defeated
                if (kingdom1.getArmy().isDefeated() || kingdom2.getArmy().isDefeated()) {
                    String winner = kingdom1.getArmy().isDefeated() ? kingdom2.getName() : kingdom1.getName();
                    System.out.println("WAR ENDED! " + winner + " has WON THE WAR!");
                    synchronized (this) {
                        warEnded = true;
                        notifyAll(); // Notify any threads waiting for the war to end
                    }
                    warActive = false;
                    break;
                }

                // Wait before next battle with random intervals
                int battleInterval = 2000 + random.nextInt(2000); // 2-4 seconds between battles
                Thread.sleep(battleInterval);
            }
        } catch (InterruptedException e) {
            // War thread interrupted, exit gracefully
        }
    }

    private void battle() {
        int strength1 = kingdom1.getCombatStrength();
        int strength2 = kingdom2.getCombatStrength();

        // Get the current strength of each army
        int army1Strength = kingdom1.getArmy().getStrength();
        int army2Strength = kingdom2.getArmy().getStrength();

        System.out.println("\nBATTLE REPORT:");
        System.out.println(kingdom1.getName() + " combat strength: " + strength1 + " (Army strength: " + army1Strength + ")");
        System.out.println(kingdom2.getName() + " combat strength: " + strength2 + " (Army strength: " + army2Strength + ")");

        // Add random battle factors (luck, terrain advantage, etc.)
        double battleFactor1 = 0.8 + random.nextDouble() * 0.4; // 80-120% effectiveness
        double battleFactor2 = 0.8 + random.nextDouble() * 0.4; // 80-120% effectiveness

        // Apply battle factors
        int adjustedStrength1 = (int)(strength1 * battleFactor1);
        int adjustedStrength2 = (int)(strength2 * battleFactor2);

        // Report on random factors
        if (battleFactor1 > 1.1) {
            System.out.println(kingdom1.getName() + " has favorable battle conditions! (+" +
                    String.format("%.1f", (battleFactor1 - 1) * 100) + "%)");
        } else if (battleFactor1 < 0.9) {
            System.out.println(kingdom1.getName() + " has unfavorable battle conditions! (-" +
                    String.format("%.1f", (1 - battleFactor1) * 100) + "%)");
        }

        if (battleFactor2 > 1.1) {
            System.out.println(kingdom2.getName() + " has favorable battle conditions! (+" +
                    String.format("%.1f", (battleFactor2 - 1) * 100) + "%)");
        } else if (battleFactor2 < 0.9) {
            System.out.println(kingdom2.getName() + " has unfavorable battle conditions! (-" +
                    String.format("%.1f", (1 - battleFactor2) * 100) + "%)");
        }

        // Determine battle outcome with adjusted strengths
        if (adjustedStrength1 > adjustedStrength2) {
            int baseDamage = (adjustedStrength1 - adjustedStrength2) / 2;
            int actualDamage = baseDamage + random.nextInt(baseDamage / 2 + 1); // Add randomness to damage

            System.out.println(kingdom1.getName() + " won the battle and dealt " + actualDamage + " damage!");

            // Reduce the opponent's strength
            kingdom2.getArmy().reduceStrength(actualDamage);
            System.out.println(kingdom2.getName() + " army strength reduced to: " + kingdom2.getArmy().getStrength());

            // Random chance for critical defeat
            boolean criticalDefeat = random.nextDouble() < 0.15; // 15% chance

            // If damage is significant, army strength is too low, or critical defeat, mark as defeated
            if (actualDamage > 20 || kingdom2.getArmy().getStrength() <= 0 ||
                    (criticalDefeat && kingdom2.getArmy().getStrength() < 15)) {
                kingdom2.getArmy().setDefeated(true);
                if (criticalDefeat) {
                    System.out.println(kingdom2.getName() + " suffered a CRITICAL DEFEAT!");
                }
                System.out.println(kingdom2.getName() + " ARMY HAS BEEN DEFEATED!");
            }
        } else if (adjustedStrength2 > adjustedStrength1) {
            int baseDamage = (adjustedStrength2 - adjustedStrength1) / 2;
            int actualDamage = baseDamage + random.nextInt(baseDamage / 2 + 1); // Add randomness to damage

            System.out.println(kingdom2.getName() + " won the battle and dealt " + actualDamage + " damage!");

            // Reduce the opponent's strength
            kingdom1.getArmy().reduceStrength(actualDamage);
            System.out.println(kingdom1.getName() + " army strength reduced to: " + kingdom1.getArmy().getStrength());

            // Random chance for critical defeat
            boolean criticalDefeat = random.nextDouble() < 0.10; // 15% chance

            // If damage is significant, army strength is too low, or critical defeat, mark as defeated
            if (actualDamage > 20 || kingdom1.getArmy().getStrength() <= 0 ||
                    (criticalDefeat && kingdom1.getArmy().getStrength() < 15)) {
                kingdom1.getArmy().setDefeated(true);
                if (criticalDefeat) {
                    System.out.println(kingdom1.getName() + " suffered a CRITICAL DEFEAT!");
                }
                System.out.println(kingdom1.getName() + " ARMY HAS BEEN DEFEATED!");
            }
        } else {
            // In case of a draw, add a small random chance for one side to gain advantage
            if (random.nextDouble() < 0.3) { // 30% chance to break the draw
                String advantagedKingdom = random.nextBoolean() ? kingdom1.getName() : kingdom2.getName();
                System.out.println("Though evenly matched, " + advantagedKingdom + " gained a slight advantage in the draw!");

                int smallDamage = random.nextInt(5) + 1; // 1-5 damage

                if (advantagedKingdom.equals(kingdom1.getName())) {
                    kingdom2.getArmy().reduceStrength(smallDamage);
                    System.out.println(kingdom2.getName() + " army strength reduced to: " + kingdom2.getArmy().getStrength());
                } else {
                    kingdom1.getArmy().reduceStrength(smallDamage);
                    System.out.println(kingdom1.getName() + " army strength reduced to: " + kingdom1.getArmy().getStrength());
                }
            } else {
                System.out.println("The battle ended in a draw! Both armies remain at their current strength.");
            }
        }
        System.out.println();
    }
}