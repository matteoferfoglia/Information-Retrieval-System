Stop words dataset
==================

## License

Files in this folder were taken from https://www.kaggle.com/heeraldedhia/stop-words-in-28-languages
(downloaded on 8th January 2022) and distributed under the
[GNU General Public License, version 2](http://www.gnu.org/licenses/old-licenses/gpl-2.0.en.html)
(as declared by the [source](https://www.kaggle.com/heeraldedhia/stop-words-in-28-languages) of the files on the date
they were downloaded).

## Description

The description of the dataset is copied from
its [source](https://www.kaggle.com/heeraldedhia/stop-words-in-28-languages):

> ### STOPWORDS
>Stopwords are the words in any language which does not add much meaning to a sentence. They can safely be ignored without sacrificing the meaning of the sentence. For some search engines, these are some of the most common, short function words, such as the, is, at, which, and on. In this case, stop words can cause problems when searching for phrases that include them, particularly in names such as “The Who” or “Take That”.
>
>#### When to remove stop words?
>If we have a task of text classification or sentiment analysis then we should remove stop words as they do not provide any information to our model, i.e keeping out unwanted words out of our corpus, but if we have the task of language translation then stopwords are useful, as they have to be translated along with other words.
>
>There is no hard and fast rule on when to remove stop words. But I would suggest removing stop words if our task to be performed is one of Language Classification, Spam Filtering, Caption Generation, Auto-Tag Generation, Sentiment analysis, or something that is related to text classification.
> On the other hand, if our task is one of Machine Translation, Question-Answering problems, Text Summarization, Language Modeling, it’s better not to remove the stop words as they are a crucial part of these applications.
>
>#### Pros and Cons:
>One of the first things that we ask ourselves is what are the pros and cons of any task we perform. Let’s look at some of the pros and cons of stop word removal in NLP.
>
>##### pros:
> - Stop words are often removed from the text before training deep learning and machine learning models since stop words occur in abundance, hence providing little to no unique information that can be used for classification or clustering.
> - On removing stopwords, dataset size decreases, and the time to train the model also decreases without a huge impact on the accuracy of the model.
> - Stopword removal can potentially help in improving performance, as there are fewer and only significant tokens left. Thus, the classification accuracy could be improved
>##### cons:
>Improper selection and removal of stop words can change the meaning of our text. So we have to be careful in choosing our stop words.
> Ex: “ This movie is not good.”
> If we remove (not) in pre-processing step the sentence (this movie is good) indicates that it is positive which is wrongly interpreted.
>
>#### Available languages
> - Arabic
> - Bulgarian
> - Catalan
> - Czech
> - Danish
> - Dutch
> - English
> - Finnish
> - French
> - German
> - Gujarati
> - Hindi
> - Hebrew
> - Hungarian
> - Indonesian
> - Malaysian
> - Italian
> - Norwegian
> - Polish
> - Portuguese
> - Romanian
> - Russian
> - Slovak
> - Spanish
> - Swedish
> - Turkish
> - Ukrainian
> - Vietnamese