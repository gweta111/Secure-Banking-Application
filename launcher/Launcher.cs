using System;
using System.Diagnostics;
using System.IO;

namespace SecureBankApp
{
    class Program
    {
        static void Main(string[] args)
        {
            Console.Title = "Secure Banking Application - Robert Kadyamusuma (H250298W)";
            
            string baseDir = AppDomain.CurrentDomain.BaseDirectory;
            Directory.SetCurrentDirectory(baseDir);

            string jarPath = Path.Combine(baseDir, "SecureBankApp.jar");
            string binPath = Path.Combine(baseDir, "bin");
            string mainClassPath = Path.Combine(binPath, "com", "securebank", "Main.class");

            string javacExecutable = FindExecutable("javac");
            string javaExecutable = FindExecutable("java");

            // Auto-compile if neither the JAR nor compiled classes exist
            if (!File.Exists(jarPath) && !File.Exists(mainClassPath))
            {
                Console.ForegroundColor = ConsoleColor.Cyan;
                Console.WriteLine("===============================================================");
                Console.WriteLine(" First-time launch detected: Compiling Java application...    ");
                Console.WriteLine("===============================================================");
                Console.ResetColor();

                ProcessStartInfo compilePsi = new ProcessStartInfo
                {
                    FileName = javacExecutable,
                    Arguments = "-d bin -sourcepath src src/com/securebank/Main.java",
                    UseShellExecute = false
                };

                try
                {
                    using (Process p = Process.Start(compilePsi))
                    {
                        p.WaitForExit();
                        if (p.ExitCode != 0)
                        {
                            Console.ForegroundColor = ConsoleColor.Red;
                            Console.WriteLine("\n[!] Compilation error occurred. Make sure JDK is installed and on PATH.");
                            Console.ResetColor();
                            if (!Console.IsInputRedirected)
                            {
                                Console.WriteLine("\nPress any key to exit...");
                                try { Console.ReadKey(); } catch { }
                            }
                            return;
                        }
                    }
                }
                catch (Exception ex)
                {
                    Console.ForegroundColor = ConsoleColor.Red;
                    Console.WriteLine("\n[!] Could not execute javac: " + ex.Message);
                    Console.WriteLine("    Please ensure a Java Development Kit (JDK) is installed.");
                    Console.ResetColor();
                    if (!Console.IsInputRedirected)
                    {
                        Console.WriteLine("\nPress any key to exit...");
                        try { Console.ReadKey(); } catch { }
                    }
                    return;
                }
            }

            // Launch the application using java
            ProcessStartInfo psi = new ProcessStartInfo
            {
                FileName = javaExecutable,
                UseShellExecute = false
            };

            if (File.Exists(jarPath))
            {
                psi.Arguments = "-jar \"" + jarPath + "\"";
            }
            else
            {
                psi.Arguments = "-cp bin com.securebank.Main";
            }

            try
            {
                using (Process proc = Process.Start(psi))
                {
                    proc.WaitForExit();
                }
            }
            catch (Exception ex)
            {
                Console.ForegroundColor = ConsoleColor.Red;
                Console.WriteLine("\n[!] Failed to start Java runtime: " + ex.Message);
                Console.WriteLine("    Please ensure Java JRE or JDK is installed and accessible.");
                Console.ResetColor();
            }

            if (!Console.IsInputRedirected)
            {
                Console.WriteLine("\nPress any key to close this window...");
                try { Console.ReadKey(true); } catch { }
            }
        }

        private static string FindExecutable(string name)
        {
            string pathEnv = Environment.GetEnvironmentVariable("PATH") ?? "";
            string[] paths = pathEnv.Split(Path.PathSeparator);
            foreach (string dir in paths)
            {
                if (string.IsNullOrWhiteSpace(dir)) continue;
                try
                {
                    string candidate = Path.Combine(dir.Trim(), name + ".exe");
                    if (File.Exists(candidate)) return candidate;
                }
                catch { }
            }

            string userProfile = Environment.GetFolderPath(Environment.SpecialFolder.UserProfile);
            string userJdk = Path.Combine(userProfile, "jdk", "jdk-21.0.12.1+1", "bin", name + ".exe");
            if (File.Exists(userJdk)) return userJdk;

            string jbr = Path.Combine(@"C:\Program Files\JetBrains\PyCharm 2026.1.2\jbr\bin", name + ".exe");
            if (File.Exists(jbr)) return jbr;

            string javaHome = Environment.GetEnvironmentVariable("JAVA_HOME");
            if (!string.IsNullOrEmpty(javaHome))
            {
                string homeCandidate = Path.Combine(javaHome, "bin", name + ".exe");
                if (File.Exists(homeCandidate)) return homeCandidate;
            }

            return name;
        }
    }
}
