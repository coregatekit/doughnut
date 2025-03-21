<template>
  <ol class="choices daisy:flex daisy:flex-wrap daisy:flex-row daisy:justify-start daisy:list-none daisy:p-0" v-if="choices && choices.length > 0">
    <li
      class="choice daisy:w-[46%] daisy:min-h-[80px] daisy:m-[2%] daisy:sm:w-full"
      v-for="(choice, index) in choices"
      :key="index"
    >
      <button
        :class="[
          'daisy:w-full daisy:h-full daisy:flex daisy:justify-center daisy:items-center',
          'daisy:rounded-lg daisy:bg-base-200',
          'daisy:hover:bg-primary daisy:hover:text-primary-content',
          'daisy:focus:outline-hidden daisy:focus:ring-2 daisy:focus:ring-primary',
          'daisy:disabled:opacity-65 daisy:transition-colors daisy:select-none',
          {
            'is-correct': isOptionCorrect(index),
            'is-incorrect': !isOptionCorrect(index),
            'is-selected': isSelectedOption(index),
          }
        ]"
        @click.once="submitAnswer({ choiceIndex: index })"
        :disabled="disabled"
      >
        <div
          v-html="getChoiceHtml(choice)"
          class="daisy:whitespace-normal daisy:break-words"
          @click.capture.prevent="handleInnerClick"
        />
      </button>
    </li>
  </ol>
</template>



<script lang="ts">
import type { AnswerDTO } from "@/generated/backend"
import { defineComponent } from "vue"
import markdownizer from "../form/markdownizer"

export default defineComponent({
  props: {
    choices: {
      type: Array<string>,
    },
    correctChoiceIndex: Number,
    answerChoiceIndex: Number,
    disabled: Boolean,
  },
  emits: ["answer"],
  data() {
    return {
      answer: "" as string,
    }
  },
  methods: {
    handleInnerClick(event: Event) {
      // Prevent any link clicks from navigating
      if (event.target instanceof HTMLAnchorElement) {
        event.preventDefault()
        event.stopPropagation()
      }
    },
    isSelectedOption(optionIndex: number) {
      return this.answerChoiceIndex === optionIndex
    },
    isOptionCorrect(index: number) {
      return index === this.correctChoiceIndex
    },
    async submitAnswer(answerData: AnswerDTO) {
      this.$emit("answer", answerData)

      // This ensures that any tapped button is blurred
      this.$nextTick(() => {
        const active = document.activeElement
        if (active instanceof HTMLElement) {
          active.blur()
        }
      })
    },
    getChoiceHtml(choice: string) {
      return markdownizer.markdownToHtml(choice)
    },
  },
})
</script>

<style scoped lang="sass">
.choices
  display: flex
  flex-wrap: wrap
  flex-direction: row
  justify-content: flex-start
  list-style-type: none
  padding-left: 0

.choice
  width: 46%
  min-height: 80px
  margin: 2%
  @media(max-width: 500px)
    width: 100%

.is-correct
  background-color: #00ff00 !important

.is-selected
  font-weight: bold
  background-color: orange !important

button, a, input
  border: 0
  -webkit-tap-highlight-color: rgba(0,0,0,0)
  -webkit-touch-callout: none
  -webkit-user-select: none
</style>
