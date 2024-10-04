import { useState, useEffect } from "react";
import axios from "axios";
import CountUp from "react-countup";
import { LuCopy } from "react-icons/lu";
import toast from "react-hot-toast";
import { useFileSystem } from "@/context/FileContext"; // Importing the File context

const ComplexityView = () => {
  const { activeFile } = useFileSystem(); // Getting the active file which contains the code
  const [complexity, setComplexity] = useState<number[] | null>(null);
  const [loopDepth, setLoopDepth] = useState<number | null>(null); // State for loop depth
  const [isLoading, setIsLoading] = useState<boolean>(false);
  const [error, setError] = useState<string | null>(null);
  const [color, setColor] = useState<string>("");

  // Handle complexity calculation when code is available in activeFile
  useEffect(() => {
    if (activeFile?.content) {
      calculateComplexity(activeFile.content);
      calculateLoopDepth(activeFile.content); // Calculate loop depth whenever code changes
    }
  }, [activeFile?.content]);

  const calculateComplexity = (code: string) => {
    setIsLoading(true);
    setError(null);

    axios
      .post("http://localhost:8080/calculate-maintainability-index", code, {
        headers: {
          "Content-Type": "text/plain",
        },
      })
      .then((response) => {
        const result = response.data;

        if (result[0] >= 0 && result[0] <= 9) {
          setColor("-red-500");
        } else if (result[0] >= 10 && result[0] <= 19) {
          setColor("-yellow-500");
        } else if (result[0] >= 20 && result[0] <= 100) {
          setColor("-green-500");
        }

        setComplexity(result);
      })
      .catch((error) => {
        setError("Error calculating maintainability. Please check your input and try again.");
        console.error("Error calculating complexity:", error);
      })
      .finally(() => {
        setIsLoading(false);
      });
  };

  // New function to calculate loop depth
  const calculateLoopDepth = (code: string) => {
    setIsLoading(true);
    setError(null);

    axios
      .post("http://localhost:8080/calculate-loop-depth", { code })
      .then((response) => {
        setLoopDepth(response.data);
      })
      .catch((error) => {
        setError("Error calculating loop depth. Please check your input and try again.");
        console.error("Error calculating loop depth:", error);
      })
      .finally(() => {
        setIsLoading(false);
      });
  };

  const copyOutput = () => {
    if (complexity && loopDepth !== null) {
      navigator.clipboard.writeText(JSON.stringify({ complexity, loopDepth }));
      toast.success("Output copied to clipboard");
    }
  };

  return (
    <div className="flex flex-col items-center gap-2 p-4">
      <h1 className="view-title">Complexity Checker</h1>
      <div className="w-full flex-grow resize-none overflow-y-auto rounded-md border-none bg-darkHover p-2 text-white outline-none">
        {error && <p className="text-red-500">{error}</p>}
        {isLoading && <p className="text-gray-400">Calculating complexity...</p>}
        {complexity && (
          <>
            <div className="flex flex-1 gap-x-6">
              {/* Maintainability Index */}
              <div className="flex-1">
                <div className={`text-2xl font-extrabold text${color} mb-2`}>
                  <CountUp start={0} end={complexity[0]} duration={5} />
                </div>
                <div className="text-xs uppercase tracking-[1px]">
                  Maintainability Index
                </div>
              </div>

              {/* Cyclomatic Complexity */}
              <div className="flex-1">
                <div className={`text-2xl font-extrabold text${color} mb-2`}>
                  <CountUp start={0} end={complexity[1]} duration={5} />
                </div>
                <div className="text-xs uppercase tracking-[1px]">
                  Cyclomatic Complexity
                </div>
              </div>

              {/* Lines of Code */}
              <div className="flex-1">
                <div className={`text-2xl font-extrabold text${color} mb-2`}>
                  <CountUp start={0} end={complexity[2]} duration={5} />
                </div>
                <div className="text-xs uppercase tracking-[1px]">
                  Lines of Code
                </div>
              </div>
              
              {/* Nested Loop Depth */}
              {loopDepth !== null && (
                <div className="flex-1">
                  <div className="text-2xl font-extrabold text-green-300 mb-2">
                    <CountUp start={0} end={loopDepth} duration={5} />
                  </div>
                  <div className="text-xs uppercase tracking-[1px]">
                    Nested Loop Depth
                  </div>
                </div>
              )}
            </div>
            <br />
            {/* Render the code as plain text without syntax highlighting */}
            <pre className="bg-gray-800 text-white p-2 rounded overflow-auto">
              {activeFile?.content}
            </pre>
          </>
        )}
      </div>
      <button onClick={copyOutput} className="px-4 py-2 text-sm bg-gray-800 text-white rounded">
        Copy Output <LuCopy size={18} />
      </button>
    </div>
  );
};

export default ComplexityView;

// import { useState, useEffect } from "react";
// import axios from "axios";
// import CountUp from "react-countup";
// import { LuCopy } from "react-icons/lu";
// import toast from "react-hot-toast";
// import { useFileSystem } from "@/context/FileContext"; // Importing the File context

// const ComplexityView = () => {
//   const { activeFile } = useFileSystem(); // Getting the active file which contains the code
//   const [complexity, setComplexity] = useState<number[] | null>(null);
//   const [isLoading, setIsLoading] = useState<boolean>(false);
//   const [error, setError] = useState<string | null>(null);
//   const [color, setColor] = useState<string>("");

//   // Handle complexity calculation when code is available in activeFile
//   useEffect(() => {
//     if (activeFile?.content) {
//       calculateComplexity(activeFile.content);
//     }
//   }, [activeFile?.content]);

//   const calculateComplexity = (code: string) => {
//     setIsLoading(true);
//     setError(null);

//     axios
//       .post("http://localhost:8080/calculate-maintainability-index", code, {
//         headers: {
//           "Content-Type": "text/plain",
//         },
//       })
//       .then((response) => {
//         const result = response.data;

//         if (result[0] >= 0 && result[0] <= 9) {
//           setColor("-red-500");
//         } else if (result[0] >= 10 && result[0] <= 19) {
//           setColor("-yellow-500");
//         } else if (result[0] >= 20 && result[0] <= 100) {
//           setColor("-green-500");
//         }

//         setComplexity(result);
//       })
//       .catch((error) => {
//         setError("Error calculating maintainability. Please check your input and try again.");
//         console.error("Error calculating complexity:", error);
//       })
//       .finally(() => {
//         setIsLoading(false);
//       });
//   };

//   const copyOutput = () => {
//     if (complexity) {
//       navigator.clipboard.writeText(JSON.stringify(complexity));
//       toast.success("Output copied to clipboard");
//     }
//   };

//   return (
//     <div className="flex flex-col items-center gap-2 p-4">
//       <h1 className="view-title">Complexity Checker</h1>
//       <div className="w-full flex-grow resize-none overflow-y-auto rounded-md border-none bg-darkHover p-2 text-white outline-none">
//         {error && <p className="text-red-500">{error}</p>}
//         {isLoading && <p className="text-gray-400">Calculating complexity...</p>}
//         {complexity && (
//           <>
//             <div className="flex flex-1 gap-x-6">
//               <div className="flex-1">
//                 <div className={`text-2xl font-extrabold text${color} mb-2`}>
//                   <CountUp start={0} end={complexity[0]} duration={5} />
//                 </div>
//                 <div className="text-xs uppercase tracking-[1px]">
//                   Maintainability Index
//                 </div>
//               </div>
//               <div className="flex-1">
//                 <div className={`text-2xl font-extrabold text${color} mb-2`}>
//                   <CountUp start={0} end={complexity[1]} duration={5} />
//                 </div>
//                 <div className="text-xs uppercase tracking-[1px]">
//                   Cyclomatic Complexity
//                 </div>
//               </div>
//               <div className="flex-1">
//                 <div className={`text-2xl font-extrabold text${color} mb-2`}>
//                   <CountUp start={0} end={complexity[2]} duration={5} />
//                 </div>
//                 <div className="text-xs uppercase tracking-[1px]">
//                   Lines of Code
//                 </div>
//               </div>
//             </div>
//             <br />
//             {/* Render the code as plain text without syntax highlighting */}
//             <pre className="bg-gray-800 text-white p-2 rounded overflow-auto">
//               {activeFile?.content}
//             </pre>
//           </>
//         )}
//       </div>
//       <button onClick={copyOutput} className="px-4 py-2 text-sm bg-gray-800 text-white rounded">
//         Copy Output <LuCopy size={18} />
//       </button>
//     </div>
//   );
// };

// export default ComplexityView;
