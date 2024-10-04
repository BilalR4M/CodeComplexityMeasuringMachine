import { useState, FC } from "react"
import axios from "axios"
import { Prism as SyntaxHighlighter } from "react-syntax-highlighter"
import { vscDarkPlus } from "react-syntax-highlighter/dist/esm/styles/prism"
import jsPDF from "jspdf"

interface Complexity {
    vocabulary: number
    length: number
    volume: number
    effort: number
    difficulty: number
}

const HalsteadMetricCalculator: FC = () => {
    const [code, setCode] = useState<string>("")
    const [complexity, setComplexity] = useState<Complexity | null>(null)
    const [isLoading, setIsLoading] = useState<boolean>(false)
    const [error, setError] = useState<string | null>(null)

    const handleCalculate = (): void => {
        setIsLoading(true)
        setError(null)

        axios
            .post("http://localhost:8080/calculate-halstead", code, {
                headers: {
                    "Content-Type": "text/plain",
                },
            })
            .then((response) => {
                const data = response.data

                if (data && data.effort !== undefined) {
                    setComplexity(data)
                } else {
                    setError("Invalid response from server.")
                }
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

        const title = "Code Complexity Measuring Machine"
        const description = `
Halstead metrics are a set of software metrics introduced by Maurice Halstead to measure the complexity of a program. These metrics provide insights into various aspects of the code, including its size, difficulty, and the effort required to understand or maintain it. The following PDF includes the calculated Halstead metrics based on the provided code.
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
        if (complexity) {
            pdf.setFontSize(16)
            pdf.setFont("helvetica", "bold")
            pdf.text("Calculated Halstead Metrics:", marginLeft, yPosition)
            yPosition += 25

            pdf.setFontSize(12)
            pdf.setFont("helvetica", "normal")
            const metrics = [
                `Vocabulary: ${complexity.vocabulary}`,
                `Length: ${complexity.length}`,
                `Volume: ${complexity.volume.toFixed(2)}`,
                `Effort: ${complexity.effort.toFixed(2)}`,
                `Difficulty: ${complexity.difficulty.toFixed(2)}`,
            ]
            metrics.forEach(metric => {
                pdf.text(metric, marginLeft + 20, yPosition)
                yPosition += 20
            })

            yPosition += 10
        }

       
        // Add Halstead Metrics Description
        pdf.setFontSize(16)
        pdf.setFont("helvetica", "bold")
        pdf.text("Halstead Metrics Description:", marginLeft, yPosition)
        yPosition += 25

        pdf.setFontSize(12)
        pdf.setFont("helvetica", "normal")
        const metricsDescription = [
            {
                title: "Length (N)",
                desc: "The total number of operators and operands in the code.",
            },
            {
                title: "Vocabulary",
                desc: "The number of unique operators and operands.",
            },
            {
                title: "Volume",
                desc: "Represents the size of the code, calculated as N * log2(Vocabulary).",
            },
            {
                title: "Difficulty",
                desc: "Measures how difficult the code is to understand, calculated as (Unique Operators / 2) * (Total Operands / Unique Operands).",
            },
            {
                title: "Effort",
                desc: "The effort required to develop or maintain the code, calculated as Volume * Difficulty.",
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
        pdf.setTextColor(0, 0, 255)
        pdf.text(code, 0, 10)

        

        // Save the PDF
        pdf.save("halstead_metrics_output.pdf")
    }

    return (
        <div className="flex min-h-screen items-center justify-center bg-gray-800">
            <div className="w-full max-w-2xl rounded-lg bg-gray-900 p-6 shadow-md">
                <h1 className="mb-6 text-center text-2xl font-bold">
                    Halstead Metric Calculator
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
                                Calculated Halstead Metrics
                            </h2>
                            <ul className="list-disc pl-5 space-y-2">
                                <li className="text-lg text-500">
                                    <strong>Vocabulary:</strong>
                                    <span className="ml-2 text-green-300 font-semibold">
                                        {complexity.vocabulary
                                            ? complexity.vocabulary
                                            : "Data not available"}
                                    </span>
                                </li>
                                <li className="text-lg text-500">
                                    <strong>Length:</strong>
                                    <span className="ml-2 text-green-300 font-semibold">
                                        {complexity.length
                                            ? complexity.length
                                            : "Data not available"}
                                    </span>
                                </li>
                                <li className="text-lg text-500">
                                    <strong>Volume:</strong>
                                    <span className="ml-2 text-green-300 font-semibold">
                                        {complexity.volume
                                            ? complexity.volume.toFixed(2)
                                            : "Data not available"}
                                    </span>
                                </li>
                                <li className="text-lg text-500">
                                    <strong>Effort:</strong>
                                    <span className="ml-2 text-green-300 font-semibold">
                                        {complexity.effort
                                            ? complexity.effort.toFixed(2)
                                            : "Data not available"}
                                    </span>
                                </li>
                                <li className="text-lg text-500">
                                    <strong>Difficulty:</strong>
                                    <span className="ml-2 text-green-300 font-semibold">
                                        {complexity.difficulty
                                            ? complexity.difficulty.toFixed(2)
                                            : "Data not available"}
                                    </span>
                                </li>
                            </ul>
                        </div>
                    </>
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
                        Halstead Metrics Description
                    </h2>
                    <ul className="list-disc pl-5">
                        <li className="text-lg text-500">
                            <strong className="text-indigo-500">Length (N):</strong> This is the total
                            number of operators and operands in the code.
                        </li>
                        <li className="text-lg text-500">
                            <strong className="text-indigo-500">Vocabulary:</strong> The number of unique
                            operators and operands.
                        </li>
                        <li className="text-lg text-500">
                            <strong className="text-indigo-500">Volume:</strong> Represents the size of the
                            code.
                        </li>
                        <li className="text-lg text-500">
                            <strong className="text-indigo-500">Difficulty:</strong> Measures how difficult
                            the code is to understand.
                        </li>
                        <li className="text-lg text-500">
                            <strong className="text-indigo-500">Effort:</strong> The effort required to
                            develop or maintain the code.
                        </li>
                    </ul>
                </div>
            </div>
        </div>
    )
}

export default HalsteadMetricCalculator
