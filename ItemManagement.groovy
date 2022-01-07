/*
This script was built for use with my "6.0 - Modular Storage With Overflow" schematic, available at https://steamcommunity.com/sharedfiles/filedetails/?id=2710693160
You may want to add new methods in the Printer class, but right now it can handle Control, Set, Operator, Sensor, and Jump commands.
It has built-in line tracking, so you can dynamically assign jump destinations without much hassle.
Use Printer.debug instead of Printer.print to include the line numbers, for help debugging.
You should be able to copy/paste the whole output directly into Mindustry, minus the exit code, and have it work.
I made this for fun, in one night, so feel free to give advice or make requests, but I'm not promising any support.
I suggest running it in Intellij IDEA, with the Groovy 4.0 library.
You may use any or all of this code, with or without attribution, for any purpose.
 */

static void main(String[] args) {
    List resources = [
            "copper",
            "lead",
            "titanium",
            "coal",
            "sand",
            "scrap",
            "spore-pod",
            "metaglass",
            "graphite",
            "thorium",
            "silicon",
            "plastanium",
            "phase-fabric",
            "surge-alloy",
            "blast-compound",
            "pyratite"
    ]
    Printer myPrinter = new Printer();
    List unloaders = ["unloader1", "unloader2", "unloader3"]
    String storage = "vault1"


    //First, we figure out what the max inventory size of the target inventory is and assign it to a local variable named maxStorage.
    myPrinter.print("sensor maxStorage ${storage} @itemCapacity")
    //Next, we initialize a variable that will be our max allowed storage.
    myPrinter.print("set allowedStorage 0")
    //Then we make allowed storage 50 less than max capacity, so we will have some wiggle room.
    myPrinter.print("op sub allowedStorage maxStorage 50")
    //Then, we make sure our unloaders aren't running unless we are flushing resources.
    unloaders.each {
        unloader ->
            myPrinter.print(myPrinter.buildControl("enabled", unloader, "flushing"))
    }
    //Now, we make sensor lines for each resource, to find out how much of each thing is currently in the storage.
    //Each one creates a variable whose name is identical to the gamename of the resource and assigns the number to it.
    resources.each {
        entry ->
            String resource = entry
            String target = storage
            myPrinter.print(myPrinter.buildSensor(resource, target))
    }
    //Next, we determine whether each resource is over the limit. The new variable created will be a numeric boolean (0 for false, 1 for true) with the resource name and "Full" after it.
    resources.each{
        entry ->
            String operation = "greaterThanEq"
            String variable1 = "${entry}Full"
            String variable2 = "${entry}"
            String value = "allowedStorage"
            myPrinter.print(myPrinter.buildOperation(operation, variable1, variable2, value))
    }
    //Hoo boy, now the "fun" part. We need to have a jump that'll bypass the block for each resource if it isn't full.
    //This requires us to know how many lines to skip, as well as what line we're on. Fortunately we've been keeping track of that.
    //Each block will configure each unloader for the resource, then enable them all by changing the "flushing" variable to 1.
    //We're testing if they're NOT full, in which case we jump over the executions.
    //Otherwise, we execute them and proceed, terminating the loop each time one's found to be full.
    //So that's 1 line per unloader, and 1 line for the variable change, 1 line for the end, and 1 line to get it to the next.
    //This will have the side effect of prioritizing resources sooner in the list. Change the order if you want.
    //Now order matters, so we are putting in our commands after the jump.
    resources.each{
        entry ->
            int destination = myPrinter.lineNumber + unloaders.size() + 3
            String variable = "${entry}Full"
            String operator = "equal"
            String value = "0"
            myPrinter.print(myPrinter.buildJump(destination, variable, operator, value))
            String action = "configure"
            String controlValue = "@${entry}" //Gotta give it the @ treatment since it's looking for a specific ingame constant.
            unloaders.each {
                unloader ->
                    myPrinter.print(myPrinter.buildControl(action, unloader, controlValue))
            }
            myPrinter.print(myPrinter.buildSet("flushing", "1"))
            myPrinter.print("end")
                }
    //So that's the actions done for each one. Now, what do we do if none of our resources are full?
    //We turn the unloaders off.
    myPrinter.print(myPrinter.buildSet("flushing", "0"))

}




class Printer {
    int lineNumber = 0
    String type = ""
    String statement = ""

    /***
     * This increments the line number, which is important to keep ahold of for jump commands.
     * @param statement (What we're printing)
     * @return
     */
    String print(String line) {
        this.lineNumber ++
        println line
        return
    }
    String debug(String line) {
        this.lineNumber ++
        println "${this.lineNumber} ${line}"
        return
    }

    /***
     *
     * @param resource (copper, lead, etc)
     * @param target (vault1, container1)
     * @return
     */
    String buildSensor(String resource, String target) {
        return "sensor ${resource} ${target} @${resource}";
    }
    /***
     *
     * @param action ("configure", "enable," etc)
     * @param target ("unloader1")
     * @param value ("true", "@copper", "1")
     * @return
     */
    String buildControl(String action, String target, String value) {
        return "control ${action} ${target} ${value} 0 0 0";
    }
/***
 *
 * @param destination - The line to which we'll be jumping
 * @param variable - The variable being tested
 * @param operator - The test (equals, greater than, etc)
 * @param value - The criteria for the test (850, @copper, etc)
 * @return
 */
    String buildJump(int destination, String variable, String operator, String value) {
        return "jump ${destination} ${operator} ${variable} ${value}";
    }
    String buildSet(String key, String value) {
        return "set ${key} ${value}";
    }
    String buildOperation(String operation, String variable1, String variable2, String value) {
        return "op ${operation} ${variable1} ${variable2} ${value}";
    }
    String end() {
        return "end"
    }

}

