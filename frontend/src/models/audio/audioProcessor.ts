export interface AudioProcessor {
  processAudioData: (newData: Float32Array[]) => void
  getAudioData: () => Float32Array[]
  start: () => void
  stop: () => Promise<File>
  flush: () => Promise<void>
}

export interface AudioChunk {
  data: File
  incomplete: boolean
}

class AudioProcessorImpl implements AudioProcessor {
  private audioData: Float32Array[] = []
  private lastProcessedArrayIndex = 0
  private lastProcessedInternalIndex = 0
  private processorTimer: NodeJS.Timeout | null = null
  private silenceCounter = 0
  private readonly SILENCE_THRESHOLD = 0.01
  private readonly SILENCE_DURATION_THRESHOLD: number

  constructor(
    private readonly sampleRate: number,
    private readonly processorCallback: (
      chunk: AudioChunk
    ) => Promise<string | undefined>
  ) {
    this.SILENCE_DURATION_THRESHOLD = 3 * sampleRate
  }

  private isSilent(data: Float32Array): boolean {
    let sum = 0
    for (let i = 0; i < data.length; i++) {
      sum += Math.abs(data[i] ?? 0)
    }
    const avg = sum / data.length
    return avg < this.SILENCE_THRESHOLD
  }

  private async processAndCallback(
    isIncomplete: boolean = true
  ): Promise<void> {
    if (this.audioData.length > this.lastProcessedArrayIndex) {
      const dataToProcess = this.audioData.slice(this.lastProcessedArrayIndex)
      this.lastProcessedArrayIndex = this.audioData.length
      const isAllSilent = dataToProcess.every((chunk) => this.isSilent(chunk))
      if (!isAllSilent) {
        const file = createAudioFile(dataToProcess, this.sampleRate, true)
        const timestamp = await this.processorCallback({
          data: file,
          incomplete: isIncomplete,
        })

        // Update indices based on timestamp if provided
        if (timestamp) {
          const processedSeconds = parseTimestamp(timestamp)
          if (processedSeconds !== undefined) {
            const processedSamples = Math.floor(
              processedSeconds * this.sampleRate
            )
            let remainingSamples = processedSamples

            // Find the new array and internal indices
            for (let i = 0; i < this.audioData.length; i++) {
              const arrayLength = this.audioData[i]?.length ?? 0
              if (remainingSamples >= arrayLength) {
                remainingSamples -= arrayLength
              } else {
                this.lastProcessedArrayIndex = i
                this.lastProcessedInternalIndex = remainingSamples
                break
              }
            }
          } else {
            // Fallback if timestamp parsing fails
            this.lastProcessedArrayIndex = this.audioData.length
            this.lastProcessedInternalIndex = 0
          }
        } else {
          // If no timestamp provided, process everything
          this.lastProcessedArrayIndex = this.audioData.length
          this.lastProcessedInternalIndex = 0
        }
      }
    }
  }

  private startTimer(): void {
    this.processorTimer = setInterval(() => {
      this.processAndCallback()
    }, 60 * 1000)
  }

  processAudioData(newData: Float32Array[]): void {
    newData.forEach((chunk) => {
      if (this.isSilent(chunk)) {
        this.silenceCounter += chunk.length
        if (this.silenceCounter >= this.SILENCE_DURATION_THRESHOLD) {
          this.flush()
          this.silenceCounter = 0
        }
      } else {
        this.silenceCounter = 0
      }

      this.audioData.push(chunk)
    })
  }

  start(): void {
    this.startTimer()
  }

  async stop(): Promise<File> {
    if (this.processorTimer) {
      clearInterval(this.processorTimer)
      this.processorTimer = null
    }
    await this.flush()
    return createAudioFile(this.audioData, this.sampleRate, false)
  }

  getAudioData(): Float32Array[] {
    return this.audioData
  }

  async flush(): Promise<void> {
    if (this.processorTimer) {
      clearInterval(this.processorTimer)
      this.startTimer()
    }
    await this.processAndCallback(false)
  }
}

export const createAudioProcessor = (
  sampleRate: number,
  processorCallback: (chunk: AudioChunk) => Promise<string | undefined>
): AudioProcessor => {
  return new AudioProcessorImpl(sampleRate, processorCallback)
}

// Helper function to create audio files
const createAudioFile = (
  data: Float32Array[],
  sampleRate: number,
  isPartial: boolean
): File => {
  const wavBlob = encodeWAV(data, sampleRate)
  const timestamp = new Date().toISOString()
  const fileName = `recorded_audio_${isPartial ? "partial_" : ""}${timestamp}.wav`
  return new File([wavBlob], fileName, { type: "audio/wav" })
}

const encodeWAV = (samples: Float32Array[], sampleRate: number): Blob => {
  const bufferLength = samples.reduce((acc, sample) => acc + sample.length, 0)
  const buffer = new ArrayBuffer(44 + bufferLength * 2)
  const view = new DataView(buffer)

  writeString(view, 0, "RIFF")
  view.setUint32(4, 36 + bufferLength * 2, true)
  writeString(view, 8, "WAVE")
  writeString(view, 12, "fmt ")
  view.setUint32(16, 16, true)
  view.setUint16(20, 1, true)
  view.setUint16(22, 1, true)
  view.setUint32(24, sampleRate, true)
  view.setUint32(28, sampleRate * 2, true)
  view.setUint16(32, 2, true)
  view.setUint16(34, 16, true)
  writeString(view, 36, "data")
  view.setUint32(40, bufferLength * 2, true)

  let offset = 44
  samples.forEach((sample) => {
    for (let i = 0; i < sample.length; i++, offset += 2) {
      const s = Math.max(-1, Math.min(1, sample[i] ?? 0))
      view.setInt16(offset, s < 0 ? s * 0x8000 : s * 0x7fff, true)
    }
  })

  return new Blob([view], { type: "audio/wav" })
}

const writeString = (view: DataView, offset: number, string: string): void => {
  for (let i = 0; i < string.length; i++) {
    view.setUint8(offset + i, string.charCodeAt(i))
  }
}

const parseTimestamp = (timestamp: string): number | undefined => {
  const [hms, millisecondsString] = timestamp.split(",")
  if (!hms || !millisecondsString) {
    return undefined
  }
  const [hours, minutes, seconds] = hms.split(":").map(Number)
  const milliseconds = Number(millisecondsString)
  if (
    hours === undefined ||
    minutes === undefined ||
    seconds === undefined ||
    milliseconds === undefined
  ) {
    return undefined
  }
  const totalSeconds =
    (hours * 60 + minutes) * 60 + seconds + milliseconds / 1000
  return totalSeconds
}
