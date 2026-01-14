private static string DeskarteakItxaron(Bezeroak b)
{
    try {
        string line = b.PlayerReader.ReadLine();
        Console.WriteLine($"Jokalari {b.PlayerZnb} deskartea: {line}");
        return line ?? "*"; 
    } catch {
        return "*";
    }
}
