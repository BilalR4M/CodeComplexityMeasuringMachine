import { useState, FC } from "react"
import axios from "axios"
import { Prism as SyntaxHighlighter } from "react-syntax-highlighter"
import { vscDarkPlus } from "react-syntax-highlighter/dist/esm/styles/prism"
import jsPDF from "jspdf"

interface LineCounts {
    codeLines: number
    commentLines: number
    blankLines: number
    functionCount: number
}

const LineCountCalculator: FC = () => {
    const [code, setCode] = useState<string>("")
    const [lineCounts, setLineCounts] = useState<LineCounts | null>(null)
    const [isLoading, setIsLoading] = useState<boolean>(false)
    const [error, setError] = useState<string | null>(null)

    const handleCalculate = (): void => {
        setIsLoading(true)
        setError(null)

        axios
            .post("http://localhost:8080/calculate-line-count", { code })
            .then((response) => {
                setLineCounts(response.data)
            })
            .catch((error) => {
                setError(
                    "Error calculating line counts. Please check your input and try again.",
                )
                console.error("Error calculating line counts:", error)
            })
            .finally(() => {
                setIsLoading(false)
            })
    }

    const handleClear = (): void => {
        setCode("")
        setLineCounts(null)
        setError(null)
    }

    const generatePdf = async () => {
        const pdf = new jsPDF("p", "pt", "a4")

        const title = "Code Complexity Measuring Machine"
        const description = `
Line counts provide a straightforward measure of a codebase's size and complexity. By analyzing the number of code lines, comment lines, blank lines, and unique methods, developers can gain insights into the maintainability and readability of the code. The following PDF includes the calculated line counts based on the provided code.
        `

        const pageWidth = pdf.internal.pageSize.getWidth()
        const marginLeft = 40
        const maxLineWidth = pageWidth - marginLeft * 2

        // Add centered title
        pdf.setFontSize(20)
        pdf.setFont("helvetica", "bold")
        const titleWidth = pdf.getTextWidth(title)
        const titleX = (pageWidth - titleWidth) / 2
        let yPosition = 40
        pdf.text(title, titleX, yPosition)

        // Add description
        pdf.setFontSize(12)
        pdf.setFont("helvetica", "normal")
        const splitDescription = pdf.splitTextToSize(description, maxLineWidth)
        yPosition += 30
        pdf.text(splitDescription, marginLeft, yPosition)

        // Calculate the height of the description to adjust the Y position
        const descriptionHeight = splitDescription.length * 14 // Approximate height per line
        yPosition += descriptionHeight + 20

        // Add Calculated Metrics
        if (lineCounts) {
            pdf.setFontSize(16)
            pdf.setFont("helvetica", "bold")
            pdf.text("Calculated Line Counts:", marginLeft, yPosition)
            yPosition += 25

            pdf.setFontSize(12)
            pdf.setFont("helvetica", "normal")
            const metrics = [
                `Code Lines: ${lineCounts.codeLines}`,
                `Comment Lines: ${lineCounts.commentLines}`,
                `Blank Lines: ${lineCounts.blankLines}`,
                `Unique Method Count: ${lineCounts.functionCount}`,
            ]
            metrics.forEach(metric => {
                pdf.text(metric, marginLeft + 20, yPosition)
                yPosition += 20
            })

            yPosition += 10
        }

        // Add Metrics Description
        pdf.setFontSize(16)
        pdf.setFont("helvetica", "bold")
        pdf.text("Metrics Description:", marginLeft, yPosition)
        yPosition += 25

        pdf.setFontSize(12)
        pdf.setFont("helvetica", "normal")
        const metricsDescription = [
            {
                title: "Code Lines (LOC)",
                desc: "The total number of lines containing actual code, excluding comments and blank lines.",
            },
            {
                title: "Comment Lines",
                desc: "The number of lines that contain comments, which help in understanding the code.",
            },
            {
                title: "Blank Lines",
                desc: "Lines that are empty or contain only whitespace, used for improving code readability.",
            },
            {
                title: "Unique Method Count",
                desc: "The number of unique methods/functions defined in the codebase.",
            },
        ]

        metricsDescription.forEach(metric => {
            pdf.setFont("helvetica", "bold")
            pdf.text(`${metric.title}:`, marginLeft + 20, yPosition) // Title position
            pdf.setFont("helvetica", "normal")
            const splitDesc = pdf.splitTextToSize(metric.desc, maxLineWidth - 40)
            pdf.text(splitDesc, marginLeft + 20, yPosition + 10) // Align description with title
            yPosition += splitDesc.length * 14 + 20 // Increase vertical space for better readability
        })


        
        // Start a new page
        pdf.addPage()
        
        pdf.setFontSize(10)
        pdf.setTextColor(0, 0, 0)
        pdf.text(code, 0, 10)

        // Save the PDF
        pdf.save("line_count_output.pdf")
    }

    return (
        <div className="flex min-h-screen items-center justify-center bg-gray-800">
            <div className="w-full max-w-2xl rounded-lg bg-gray-900 p-6 shadow-md">
                <div className="CCMTcontainer">
                    {error && <p style={{ color: "red" }}>{error}</p>}
                </div>
                <h1 className="mb-6 text-center text-2xl font-bold">
                    Line Count Calculator
                </h1>
                <div className="relative w-[32rem]">
                    <div className="relative w-full min-w-[200px]">
                        <textarea
                            value={code}
                            onChange={(e) => setCode(e.target.value)}
                            rows={8}
                            spellCheck="false"
                            className="border-blue-gray-200 text-blue-gray-700 placeholder-shown:border-blue-gray-200 placeholder-shown:border-t-blue-gray-200 disabled:bg-blue-gray-50 peer h-full min-h-[100px] w-full !resize-none rounded-[7px] border border-t-transparent bg-transparent px-3 py-2.5 font-sans text-sm font-normal outline outline-0 transition-all placeholder-shown:border focus:border-2 focus:border-white focus:border-t-transparent focus:outline-0 disabled:resize-none disabled:border-0"
                            placeholder=" "
                        ></textarea>
                        <label className="before:content[' '] after:content[' '] text-blue-gray-400 before:border-blue-gray-200 after:border-blue-gray-200 peer-placeholder-shown:text-blue-gray-500 peer-disabled:peer-placeholder-shown:text-blue-gray-500 pointer-events-none absolute -top-1.5 left-0 flex h-full w-full select-none text-[11px] font-normal leading-tight transition-all before:pointer-events-none before:mr-1 before:mt-[6.5px] before:box-border before:block before:h-1.5 before:w-2.5 before:rounded-tl-md before:border-l before:border-t before:transition-all after:pointer-events-none after:ml-1 after:mt-[6.5px] after:box-border after:block after:h-1.5 after:w-2.5 after:flex-grow after:rounded-tr-md after:border-r after:border-t after:transition-all peer-placeholder-shown:text-sm peer-placeholder-shown:leading-[3.75] peer-placeholder-shown:before:border-transparent peer-focus:text-[11px] peer-focus:leading-tight peer-focus:text-white peer-focus:before:border-l-2 peer-focus:before:border-t-2 peer-focus:before:!border-white peer-focus:after:border-r-2 peer-focus:after:border-t-2 peer-focus:after:!border-white peer-disabled:text-transparent peer-disabled:before:border-transparent peer-disabled:after:border-transparent">
                            Enter your code here
                        </label>
                    </div>
                    <div className="flex w-full justify-between py-1.5">
                        <div className="flex gap-2">
                            <button
                                onClick={handleClear}
                                className="select-none rounded-md px-4 py-2 text-center align-middle font-sans text-xs font-bold uppercase text-white transition-all hover:bg-gray-100/10 active:bg-gray-100/20 disabled:pointer-events-none disabled:opacity-50 disabled:shadow-none"
                                type="button"
                            >
                                Clear
                            </button>
                            <button
                                onClick={handleCalculate}
                                disabled={isLoading}
                                className="select-none rounded-md bg-white px-4 py-2 text-center align-middle font-sans text-xs font-bold uppercase text-gray-900 shadow-md shadow-gray-100/10 transition-all hover:shadow-lg hover:shadow-gray-100/20 focus:opacity-[0.85] focus:shadow-none active:opacity-[0.85] active:shadow-none disabled:pointer-events-none disabled:opacity-50 disabled:shadow-none"
                                type="button"
                            >
                                {isLoading ? "Calculating..." : "Calculate Line Counts"}
                            </button>
                            {lineCounts !== null && (
                                <button
                                    onClick={generatePdf}
                                    className="select-none rounded-md bg-green-500 px-4 py-2 text-center align-middle font-sans text-xs font-bold uppercase text-white shadow-md shadow-gray-100/10 transition-all hover:shadow-lg hover:shadow-gray-100/20 focus:opacity-[0.85] focus:shadow-none active:opacity-[0.85] active:shadow-none"
                                    type="button"
                                >
                                    PDF
                                </button>
                            )}
                        </div>
                    </div>
                </div>
                {lineCounts && (
                    <div className="m-4 rounded-lg border border-gray-200 bg-gray-800 p-6 shadow-lg">
                        <h2 className="mb-4 text-2xl font-bold text-teal-600">
                            Code Metrics
                        </h2>
                        <ul className="list-disc pl-5 space-y-2">
                            <li className="text-lg text-500">
                                <strong>Code Lines:</strong>{" "}
                                <span className="ml-2 text-green-300 font-semibold">
                                    {lineCounts.codeLines ? lineCounts.codeLines : "Data not available"}
                                </span>
                            </li>
                            <li className="text-lg text-500">
                                <strong>Comment Lines:</strong>{" "}
                                <span className="ml-2 text-green-300 font-semibold">
                                    {lineCounts.commentLines ? lineCounts.commentLines : "Data not available"}
                                </span>
                            </li>
                            <li className="text-lg text-500">
                                <strong>Blank Lines:</strong>{" "}
                                <span className="ml-2 text-green-300 font-semibold">
                                    {lineCounts.blankLines ? lineCounts.blankLines : "Data not available"}
                                </span>
                            </li>
                            <li className="text-lg text-500">
                                <strong>Unique Method Count:</strong>{" "}
                                <span className="ml-2 text-green-300 font-semibold">
                                    {lineCounts.functionCount ? lineCounts.functionCount : "Data not available"}
                                </span>
                            </li>
                        </ul>
                    </div>
                )}
                {code && (
                    <div
                        id="code-snippet"
                        style={{ height: "220px", overflowY: "scroll" }}
                        className="mb-4"
                    >
                        <SyntaxHighlighter
                            language="java"
                            style={vscDarkPlus}
                            showLineNumbers={true}
                            wrapLines={true}
                        >
                            {code}
                        </SyntaxHighlighter>
                    </div>
                )}

                <div className="m-4 rounded-lg border border-gray-200 bg-gray-800 p-6 shadow-lg">
                    <h2 className="mb-4 text-2xl font-bold text-teal-600">
                        Line Counts Description
                    </h2>
                    <ul className="list-disc pl-5">
                        <li className="text-lg text-500">
                            <strong className="text-indigo-500">Code Lines (LOC):</strong> The total number of lines containing actual code, excluding comments and blank lines.
                        </li>
                        <li className="text-lg text-500">
                            <strong className="text-indigo-500">Comment Lines:</strong> The number of lines that contain comments, which help in understanding the code.
                        </li>
                        <li className="text-lg text-500">
                            <strong className="text-indigo-500">Blank Lines:</strong> Lines that are empty or contain only whitespace, used for improving code readability.
                        </li>
                        <li className="text-lg text-500">
                            <strong className="text-indigo-500">Unique Method Count:</strong> The number of unique methods/functions defined in the codebase.
                        </li>
                    </ul>
                </div>
            </div>
        </div>
    )
}

export default LineCountCalculator

// import { useState, FC } from "react"
// import axios from "axios"
// import { Prism as SyntaxHighlighter } from "react-syntax-highlighter"
// import { vscDarkPlus } from "react-syntax-highlighter/dist/esm/styles/prism"

// interface LineCounts {
//     codeLines: number
//     commentLines: number
//     blankLines: number
//     functionCount: number
// }

// const LineCountCalculator: FC = () => {
//     const [code, setCode] = useState<string>("")
//     const [lineCounts, setLineCounts] = useState<LineCounts | null>(null)
//     const [isLoading, setIsLoading] = useState<boolean>(false)
//     const [error, setError] = useState<string | null>(null)

//     const handleCalculate = (): void => {
//         setIsLoading(true)
//         setError(null)

//         axios
//             .post("http://localhost:8080/calculate-line-count", { code })
//             .then((response) => {
//                 setLineCounts(response.data)
//             })
//             .catch((error) => {
//                 setError(
//                     "Error calculating line counts. Please check your input and try again.",
//                 )
//                 console.error("Error calculating line counts:", error)
//             })
//             .finally(() => {
//                 setIsLoading(false)
//             })
//     }

//     const handleClear = (): void => {
//         setCode("")
//         setLineCounts(null)
//         setError(null)
//     }

//     return (
//         <div className="flex min-h-screen items-center justify-center bg-gray-800">
//             <div className="w-full max-w-2xl rounded-lg bg-gray-900 p-6 shadow-md">
//                 <div className="CCMTcontainer">
//                     {error && <p style={{ color: "red" }}>{error}</p>}
//                 </div>
//                 <h1 className="mb-6 text-center text-2xl font-bold">
//                     Line Count Calculator
//                 </h1>
//                 <div className="relative w-[32rem]">
//                     <div className="relative w-full min-w-[200px]">
//                         <textarea
//                             value={code}
//                             onChange={(e) => setCode(e.target.value)}
//                             rows={8}
//                             spellCheck="false"
//                             className="border-blue-gray-200 text-blue-gray-700 placeholder-shown:border-blue-gray-200 placeholder-shown:border-t-blue-gray-200 disabled:bg-blue-gray-50 peer h-full min-h-[100px] w-full !resize-none rounded-[7px] border border-t-transparent bg-transparent px-3 py-2.5 font-sans text-sm font-normal outline outline-0 transition-all placeholder-shown:border focus:border-2 focus:border-white focus:border-t-transparent focus:outline-0 disabled:resize-none disabled:border-0"
//                             placeholder=" "
//                         ></textarea>
//                         <label className="before:content[' '] after:content[' '] text-blue-gray-400 before:border-blue-gray-200 after:border-blue-gray-200 peer-placeholder-shown:text-blue-gray-500 peer-disabled:peer-placeholder-shown:text-blue-gray-500 pointer-events-none absolute -top-1.5 left-0 flex h-full w-full select-none text-[11px] font-normal leading-tight transition-all before:pointer-events-none before:mr-1 before:mt-[6.5px] before:box-border before:block before:h-1.5 before:w-2.5 before:rounded-tl-md before:border-l before:border-t before:transition-all after:pointer-events-none after:ml-1 after:mt-[6.5px] after:box-border after:block after:h-1.5 after:w-2.5 after:flex-grow after:rounded-tr-md after:border-r after:border-t after:transition-all peer-placeholder-shown:text-sm peer-placeholder-shown:leading-[3.75] peer-placeholder-shown:before:border-transparent peer-focus:text-[11px] peer-focus:leading-tight peer-focus:text-white peer-focus:before:border-l-2 peer-focus:before:border-t-2 peer-focus:before:!border-white peer-focus:after:border-r-2 peer-focus:after:border-t-2 peer-focus:after:!border-white peer-disabled:text-transparent peer-disabled:before:border-transparent peer-disabled:after:border-transparent">
//                             Enter your code here
//                         </label>
//                     </div>
//                     <div className="flex w-full justify-between py-1.5">
//                         <div className="flex gap-2">
//                             <button
//                                 onClick={handleClear}
//                                 className="select-none rounded-md px-4 py-2 text-center align-middle font-sans text-xs font-bold uppercase text-white transition-all hover:bg-gray-100/10 active:bg-gray-100/20 disabled:pointer-events-none disabled:opacity-50 disabled:shadow-none"
//                                 type="button"
//                             >
//                                 Clear
//                             </button>
//                             <button
//                                 onClick={handleCalculate}
//                                 disabled={isLoading}
//                                 className="select-none rounded-md bg-white px-4 py-2 text-center align-middle font-sans text-xs font-bold uppercase text-gray-900 shadow-md shadow-gray-100/10 transition-all hover:shadow-lg hover:shadow-gray-100/20 focus:opacity-[0.85] focus:shadow-none active:opacity-[0.85] active:shadow-none disabled:pointer-events-none disabled:opacity-50 disabled:shadow-none"
//                                 type="button"
//                             >
//                                 {isLoading
//                                     ? "Calculating..."
//                                     : "Calculate Line Counts"}
//                             </button>
//                         </div>
//                     </div>
//                 </div>
//                 {lineCounts && (
//                     <div className="m-4 rounded-lg border border-gray-200 bg-gray-800 p-6 shadow-lg">
//                         <h2 className="mb-4 text-2xl font-bold text-teal-600">
//                             Code Metrics
//                         </h2>
//                         <ul className="list-disc pl-5 space-y-2">
//                             <li className="text-lg text-500">
//                                 <strong>Code Lines:</strong>{" "}
//                                 <span className="ml-2 text-green-300 font-semibold">
//                                     {lineCounts.codeLines ? lineCounts.codeLines : "Data not available"}
//                                 </span>
//                             </li>
//                             <li className="text-lg text-500">
//                                 <strong>Comment Lines:</strong>{" "}
//                                 <span className="ml-2 text-green-300 font-semibold">
//                                     {lineCounts.commentLines ? lineCounts.commentLines : "Data not available"}
//                                 </span>
//                             </li>
//                             <li className="text-lg text-500">
//                                 <strong>Blank Lines:</strong>{" "}
//                                 <span className="ml-2 text-green-300 font-semibold">
//                                     {lineCounts.blankLines ? lineCounts.blankLines : "Data not available"}
//                                 </span>
//                             </li>
//                             <li className="text-lg text-500">
//                                 <strong>Unique Method Count:</strong>{" "}
//                                 <span className="ml-2 text-green-300 font-semibold">
//                                     {lineCounts.functionCount ? lineCounts.functionCount : "Data not available"}
//                                 </span>
//                             </li>
//                         </ul>
//                     </div>
               
//                 )}
//                 {code && (
//                     <div style={{ height: "220px", overflowY: "scroll" }}>
//                         <SyntaxHighlighter
//                             language="java"
//                             style={vscDarkPlus}
//                             showLineNumbers={true}
//                             wrapLines={true}
//                         >
//                             {code}
//                         </SyntaxHighlighter>
//                     </div>
//                 )}
//             </div>
//         </div>
//     )
// }

// export default LineCountCalculator
