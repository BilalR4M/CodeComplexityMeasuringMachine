import { FC, useState } from "react"
import axios from "axios"
import { Prism as SyntaxHighlighter } from "react-syntax-highlighter"
import { vscDarkPlus } from "react-syntax-highlighter/dist/esm/styles/prism"
import jsPDF from "jspdf"
import html2canvas from "html2canvas"

const ComplexityCalc: FC = () => {
    const [code, setCode] = useState<string>("")
    const [complexity, setComplexity] = useState<number | null>(null)
    const [isLoading, setIsLoading] = useState<boolean>(false)
    const [error, setError] = useState<string | null>(null)


    const handleCalculate = (): void => {
        setIsLoading(true)
        setError(null)

        axios
            .post("http://localhost:8080/calculate-loop-depth", { code })
            .then((response) => {
                setComplexity(response.data)
            })
            .catch((error) => {
                setError(
                    "Error calculating complexity. Please check your input and try again.",
                )
                console.error("Error calculating complexity:", error)
            })
            .finally(() => {
                setIsLoading(false)
            })
    }

    const handleClear = (): void => {
        setCode("")
        setComplexity(null)
        setError(null)
    }

    const generatePdf = async () => {
        const pdf = new jsPDF("p", "pt", "a4")
    
        const title = "Nested Loop Depth Complexity"
        
        const description = `
    Nested loop depth complexity refers to the maximum depth of nested loops in a given piece of code.It is used as a measure of how deeply loops are nested inside one another, which impacts both readability and performance. A higher depth increases time complexity, making code more computationally expensive to execute. The following shows the calculated loop depth complexity based on the provided code:
        `
    
        const pageWidth = pdf.internal.pageSize.getWidth() // Get the page width
        const marginLeft = 40
        const maxLineWidth = pageWidth - marginLeft * 2
    
        // Add centered title
        pdf.setFontSize(20)
        const titleWidth = pdf.getTextWidth(title)
        const titleX = (pageWidth - titleWidth) / 2 // Center the title
        let yPosition = 40 // Start position for the title
        pdf.text(title, titleX, yPosition)
       
        yPosition += 10

        pdf.setFontSize(10)
        pdf.setTextColor(0, 0, 0)
        pdf.text(code, 10, yPosition)

        // Start a new page
        pdf.addPage()

        // Add justified description
        pdf.setFontSize(12)
        const splitDescription = pdf.splitTextToSize(description, maxLineWidth) // Split description for line width
        yPosition += 30 // Add some space before the description
        pdf.text(splitDescription, marginLeft, yPosition) // Justify text by splitting into lines
    
        // Calculate the height of the description to adjust the Y position
        const descriptionHeight = splitDescription.length * 12 // Approximate height of each line of text
        yPosition += descriptionHeight + 20 // Adjust Y position based on description height
    
        // Capture the output container as an image using html2canvas
        const outputDiv = document.getElementById("output") as HTMLElement
        const canvas = await html2canvas(outputDiv)
        const imgData = canvas.toDataURL("image/png")
    
        // Adding the image to the PDF
        pdf.addImage(imgData, "PNG", marginLeft, yPosition, maxLineWidth, 200)
    
        yPosition += 220 // Adjust Y position after the first image
        // 
    
        // Save the PDF
        pdf.save("complexity_output.pdf")
    }
    
    return (
        <div
            className={`flex min-h-screen items-center justify-center bg-gray-800`}
        >
            <div className="w-full max-w-2xl rounded-lg bg-gray-900 p-6 shadow-md">
                <h1 className="mb-6 text-center text-2xl font-bold">
                    Loop Depth Calculator
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
                                {isLoading ? "Calculating..." : "Calculate Complexity"}
                            </button>
                            {complexity !== null && (
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
                {error && <p className="text-red-500">{error}</p>}
                {complexity !== null && (
                    <>
                        <div
                            id="output"
                            className="m-4 rounded-lg border border-gray-200 bg-gray-800 p-6 shadow-lg"
                        >
                            <h2 className="mb-4 text-2xl font-bold text-teal-600">
                                Calculated Loop Depth
                            </h2>
                            <div className="flex flex-1 gap-x-6">
                                <div className="mb-2 text-2xl font-semibold text-green-300 xl:text-4xl ">
                                    {complexity
                                        ? complexity
                                        : "Data not available"}
                                </div>
                            </div>
                        </div>
                        <br />
                        <br />
                    </>
                )}
                <br />
                {code && (
                    <div
                        id="code-snippet"
                        style={{ height: "220px", overflowY: "scroll" }}
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
            </div>
        </div>
    )
}

export default ComplexityCalc



// import { FC, useState } from "react"
// import axios from "axios"
// import { Prism as SyntaxHighlighter } from "react-syntax-highlighter"
// import { vscDarkPlus } from "react-syntax-highlighter/dist/esm/styles/prism"

// const ComplexityCalc: FC = () => {
//     const [code, setCode] = useState<string>("")
//     const [complexity, setComplexity] = useState<number | null>(null)
//     const [isLoading, setIsLoading] = useState<boolean>(false)
//     const [error, setError] = useState<string | null>(null)

//     const handleCalculate = (): void => {
//         setIsLoading(true)
//         setError(null)

//         axios
//             .post("http://localhost:8080/calculate-loop-depth", { code })
//             .then((response) => {
//                 setComplexity(response.data)
//             })
//             .catch((error) => {
//                 setError(
//                     "Error calculating complexity. Please check your input and try again.",
//                 )
//                 console.error("Error calculating complexity:", error)
//             })
//             .finally(() => {
//                 setIsLoading(false)
//             })
//     }

//     const handleClear = (): void => {
//         setCode("")
//         setComplexity(null)
//         setError(null)
//     }

//     return (
//         <div
//             className={`flex min-h-screen items-center justify-center bg-gray-800`}
//         >
//             <div className="w-full max-w-2xl rounded-lg bg-gray-900 p-6 shadow-md">
//                 <h1 className="mb-6 text-center text-2xl font-bold">
//                     Loop Depth Calculator
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
//                                 Calculate Complexity
//                             </button>
//                         </div>
//                     </div>
//                 </div>
//                 {error && <p className="text-red-500">{error}</p>}
//                 {complexity !== null && (
//                     <>
//                         <div className="m-4 rounded-lg border border-gray-200 bg-gray-800 p-6 shadow-lg">
//                             <h2 className="mb-4 text-2xl font-bold text-teal-600">
//                                 Calculated Loop Depth
//                             </h2>
//                             <div className="flex flex-1 gap-x-6">
//                                 <div className="mb-2 text-2xl font-semibold text-green-300 xl:text-4xl ">
//                                      {complexity ? complexity : "Data not available"}
//                                 </div>
//                             </div>
//                         </div>
//                         <br />
//                         <br />
//                     </>
//                 )}
//                 <br />
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

// export default ComplexityCalc
